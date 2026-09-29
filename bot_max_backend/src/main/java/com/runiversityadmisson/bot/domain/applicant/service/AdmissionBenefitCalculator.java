package com.runiversityadmisson.bot.domain.applicant.service;

import com.runiversityadmisson.bot.domain.applicant.model.benefit.Achievement;
import com.runiversityadmisson.bot.domain.applicant.model.olympiad.Olympiad;
import com.runiversityadmisson.bot.domain.applicant.model.olympiad.OlympiadBenefit;
import com.runiversityadmisson.bot.domain.applicant.model.olympiad.OlympiadBenefitRule;
import com.runiversityadmisson.bot.domain.applicant.model.olympiad.OlympiadDegree;
import com.runiversityadmisson.bot.domain.applicant.model.olympiad.OlympiadProfile;
import com.runiversityadmisson.bot.domain.applicant.model.university.Program;
import com.runiversityadmisson.bot.domain.applicant.model.university.ProgramSubject;
import com.runiversityadmisson.bot.domain.applicant.model.university.University;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Считает, что олимпиады и индивидуальные достижения дают абитуриенту на конкретном направлении.
 * Без Spring и БД: всё нужное приходит аргументами.
 *
 * <p>Для каждого диплома подбираются подходящие правила вуза, из них применяется самое выгодное
 * (БВИ → 100 баллов → баллы за ИД), для которого хватает ЕГЭ по предмету профиля.
 * Потом считается сумма ЕГЭ с учётом «100 баллов» и баллы за ИД с учётом лимита вуза.
 */
public class AdmissionBenefitCalculator {

	/** Диплом действует 4 года, следующих за годом проведения олимпиады. */
	static final int DIPLOMA_VALIDITY_YEARS = 4;

	private final int campaignYear;
	private final Map<String, String> subjectNames;

	/**
	 * @param campaignYear год приёмной кампании: от него считается срок действия дипломов
	 * @param subjectNames id предмета ЕГЭ → название, для пояснений
	 */
	public AdmissionBenefitCalculator(int campaignYear, Map<String, String> subjectNames) {
		this.campaignYear = campaignYear;
		this.subjectNames = subjectNames;
	}

	public ProgramResult calculate(Program program, Applicant applicant, List<OlympiadBenefitRule> rules) {
		List<DiplomaResult> diplomas = applicant.diplomas().stream()
				.map(diploma -> evaluate(diploma, program, applicant.egeScores(), rules))
				.toList();
		OlympiadBenefit benefit = diplomas.stream()
				.map(DiplomaResult::benefit)
				.filter(result -> result == OlympiadBenefit.BVI || result == OlympiadBenefit.SCORE_100)
				.min(Comparator.naturalOrder())
				.orElse(OlympiadBenefit.NONE);
		// БВИ и общий конкурс независимы: диплом с БВИ может ещё и дать 100 баллов в общем конкурсе.
		Map<String, DiplomaResult> hundredBySubject = new LinkedHashMap<>();
		diplomas.stream()
				.filter(DiplomaResult::hundredPoints)
				.forEach(result -> hundredBySubject.putIfAbsent(result.diploma().profile().getSubjectId(), result));

		EgeSum ege = sumEge(program, applicant.egeScores(), Map.of());
		EgeSum egeWithBenefits = sumEge(program, applicant.egeScores(), hundredBySubject);
		List<AchievementResult> achievements = scoreAchievements(
				program.getUniversity(), applicant.achievements(), diplomas);
		int achievementPoints = achievements.stream()
				.filter(AchievementResult::counted)
				.mapToInt(AchievementResult::points)
				.sum();
		return new ProgramResult(program, benefit, diplomas, ege.total(), egeWithBenefits.total(),
				egeWithBenefits.missing(), egeWithBenefits.exams(), achievements, achievementPoints,
				egeWithBenefits.total() + achievementPoints);
	}

	private DiplomaResult evaluate(Diploma diploma, Program program, Map<String, Integer> egeScores,
			List<OlympiadBenefitRule> rules) {
		OlympiadProfile profile = diploma.profile();
		int age = campaignYear - profile.getOlympiadYear();
		if (age < 0 || age > DIPLOMA_VALIDITY_YEARS) {
			return new DiplomaResult(diploma, OlympiadBenefit.NONE, null, false,
					"Диплом " + profile.getOlympiadYear() + " года не действует в приёмной кампании " + campaignYear + " года");
		}

		List<OlympiadBenefitRule> candidates = rules.stream()
				.filter(rule -> matches(rule, program, diploma))
				.sorted(Comparator.comparing(OlympiadBenefitRule::getBenefit)
						.thenComparing(OlympiadBenefitRule::getMinEgeScore, Comparator.nullsFirst(Comparator.naturalOrder())))
				.toList();
		if (candidates.isEmpty()) {
			return new DiplomaResult(diploma, OlympiadBenefit.NONE, null, false, "Не даёт льгот на этом направлении");
		}

		Integer egeScore = egeScores.get(profile.getSubjectId());
		String betterBenefitHint = null;
		for (OlympiadBenefitRule rule : candidates) {
			if (isConfirmed(rule, egeScore)) {
				boolean hundredPoints = candidates.stream().anyMatch(candidate ->
						candidate.getBenefit() == OlympiadBenefit.SCORE_100 && isConfirmed(candidate, egeScore));
				String note = describe(rule, profile);
				if (rule.getBenefit() == OlympiadBenefit.BVI && hundredPoints) {
					note += ". В общем конкурсе — 100 баллов по предмету «" + subjectName(profile.getSubjectId()) + "»";
				}
				return new DiplomaResult(diploma, rule.getBenefit(), rule.getPoints(), hundredPoints,
						betterBenefitHint == null ? note : note + ". " + betterBenefitHint);
			}
			if (betterBenefitHint == null) {
				betterBenefitHint = confirmationHint(rule, profile, egeScore);
			}
		}
		// Ни одно правило не подтверждено ЕГЭ — подсказываем самый низкий порог.
		OlympiadBenefitRule easiest = candidates.stream()
				.min(Comparator.comparing(OlympiadBenefitRule::getMinEgeScore))
				.orElseThrow();
		return new DiplomaResult(diploma, OlympiadBenefit.NONE, null, false,
				confirmationHint(easiest, profile, egeScore));
	}

	private static boolean matches(OlympiadBenefitRule rule, Program program, Diploma diploma) {
		OlympiadProfile profile = diploma.profile();
		Olympiad olympiad = profile.getOlympiad();
		return (rule.getUniversityId() == null || rule.getUniversityId().equals(program.getUniversity().getId()))
				&& (rule.getProgramId() == null || rule.getProgramId().equals(program.getId()))
				&& (rule.getOlympiadId() == null || rule.getOlympiadId().equals(olympiad.getId()))
				&& rule.getProfile().equals(profile.getProfile())
				&& rule.isVsosh() == olympiad.isVsosh()
				&& (rule.getMaxLevel() == null || profile.getLevel() != null && profile.getLevel() <= rule.getMaxLevel())
				&& (rule.getDegree() == null || rule.getDegree() == diploma.degree())
				// БВИ и 100 баллов дают только по профилям, соответствующим направлению; баллы за ИД — на любом.
				&& (rule.getBenefit() == OlympiadBenefit.ACHIEVEMENT_POINTS
						|| program.getOlympiadProfiles().contains(profile.getProfile()));
	}

	private static boolean isConfirmed(OlympiadBenefitRule rule, Integer egeScore) {
		return rule.getMinEgeScore() == null || egeScore != null && egeScore >= rule.getMinEgeScore();
	}

	private String describe(OlympiadBenefitRule rule, OlympiadProfile profile) {
		return switch (rule.getBenefit()) {
			case BVI -> "Зачисление без вступительных испытаний";
			case SCORE_100 -> "100 баллов по предмету «" + subjectName(profile.getSubjectId()) + "»";
			case ACHIEVEMENT_POINTS -> "Баллы за индивидуальные достижения: " + rule.getPoints();
			case NONE -> "Не даёт льгот на этом направлении";
		};
	}

	private String confirmationHint(OlympiadBenefitRule rule, OlympiadProfile profile, Integer egeScore) {
		String benefit = switch (rule.getBenefit()) {
			case BVI -> "БВИ";
			case SCORE_100 -> "100 баллов";
			case ACHIEVEMENT_POINTS, NONE -> "баллов за ИД";
		};
		String hint = "Для льготы «" + benefit + "» нужен ЕГЭ по предмету «" + subjectName(profile.getSubjectId())
				+ "» не ниже " + rule.getMinEgeScore();
		return egeScore == null ? hint + ", результата пока нет" : hint + ", сейчас " + egeScore;
	}

	/**
	 * Сумма ЕГЭ по предметам направления; из взаимозаменяемых предметов берётся лучший.
	 * По каждому предмету запоминается, засчитан ли он и почему.
	 */
	private EgeSum sumEge(Program program, Map<String, Integer> egeScores, Map<String, DiplomaResult> hundredBySubject) {
		Map<String, List<String>> slots = new LinkedHashMap<>();
		for (ProgramSubject subject : program.getSubjects()) {
			String slot = subject.getChoiceGroup() == null
					? "subject:" + subject.getSubjectId()
					: "group:" + subject.getChoiceGroup();
			slots.computeIfAbsent(slot, key -> new ArrayList<>()).add(subject.getSubjectId());
		}

		int total = 0;
		List<String> missing = new ArrayList<>();
		List<ExamResult> exams = new ArrayList<>();
		for (List<String> alternatives : slots.values()) {
			Map<String, Integer> counted = new LinkedHashMap<>();
			for (String subjectId : alternatives) {
				Integer score = hundredBySubject.containsKey(subjectId) ? Integer.valueOf(100) : egeScores.get(subjectId);
				if (score != null) {
					counted.put(subjectId, score);
				}
			}
			String chosen = counted.entrySet().stream()
					.max(Map.Entry.comparingByValue())
					.map(Map.Entry::getKey)
					.orElse(null);
			if (chosen == null) {
				missing.add(alternatives.stream().map(this::subjectName).collect(Collectors.joining(" или ")));
			} else {
				total += counted.get(chosen);
			}
			for (String subjectId : alternatives) {
				exams.add(examResult(subjectId, egeScores.get(subjectId), counted.get(subjectId), chosen,
						counted.get(chosen), hundredBySubject.get(subjectId)));
			}
		}
		return new EgeSum(total, missing, exams);
	}

	private ExamResult examResult(String subjectId, Integer egeScore, Integer score, String chosen, Integer chosenScore,
			DiplomaResult hundred) {
		boolean fromOlympiad = hundred != null;
		String name = subjectName(subjectId);
		if (score == null) {
			return new ExamResult(subjectId, name, null, null, false, false, "Нет результата ЕГЭ");
		}
		if (!subjectId.equals(chosen)) {
			return new ExamResult(subjectId, name, egeScore, score, false, fromOlympiad,
					"Не засчитан: выбран предмет «" + subjectName(chosen) + "» (" + chosenScore + " ≥ " + score + ")");
		}
		String note = fromOlympiad
				? "100 баллов по диплому олимпиады «" + hundred.diploma().profile().getOlympiad().getName() + "»"
				: "Результат ЕГЭ";
		return new ExamResult(subjectId, name, egeScore, score, true, fromOlympiad, note);
	}

	private List<AchievementResult> scoreAchievements(University university, List<Achievement> achievements,
			List<DiplomaResult> diplomas) {
		List<AchievementResult> claimed = new ArrayList<>();
		for (Achievement achievement : achievements) {
			Integer points = university.getAchievementPoints().get(achievement.getId());
			claimed.add(points == null
					? AchievementResult.notCounted(achievement.getId(), achievement.getName(), "Не учитывается в этом вузе")
					: new AchievementResult(achievement.getId(), achievement.getName(), points, true, null));
		}

		// Олимпиады без особых прав засчитываются как одно достижение: берём самую выгодную.
		List<DiplomaResult> olympiadPoints = diplomas.stream()
				.filter(result -> result.benefit() == OlympiadBenefit.ACHIEVEMENT_POINTS)
				.sorted(Comparator.comparing(DiplomaResult::points).reversed())
				.toList();
		for (int index = 0; index < olympiadPoints.size(); index++) {
			DiplomaResult result = olympiadPoints.get(index);
			OlympiadProfile profile = result.diploma().profile();
			String id = "olympiad:" + profile.getId();
			String name = profile.getOlympiad().getName() + " (" + profile.getName() + ")";
			claimed.add(index == 0
					? new AchievementResult(id, name, result.points(), true, null)
					: AchievementResult.notCounted(id, name, "Засчитывается только одна олимпиада"));
		}
		return applyLimit(claimed, university.getAchievementPointsMax());
	}

	/** Вуз засчитывает за ИД не больше limit баллов: самые дорогие достижения идут первыми. */
	private static List<AchievementResult> applyLimit(List<AchievementResult> claimed, int limit) {
		List<AchievementResult> ordered = claimed.stream()
				.sorted(Comparator.comparing(AchievementResult::counted).reversed()
						.thenComparing(AchievementResult::points, Comparator.reverseOrder()))
				.toList();
		List<AchievementResult> result = new ArrayList<>();
		int remaining = limit;
		for (AchievementResult item : ordered) {
			if (!item.counted()) {
				result.add(item);
			} else if (remaining == 0) {
				result.add(AchievementResult.notCounted(item.achievementId(), item.achievementName(),
						"Уже набран максимум за ИД в этом вузе (" + limit + ")"));
			} else {
				int points = Math.min(item.points(), remaining);
				remaining -= points;
				result.add(new AchievementResult(item.achievementId(), item.achievementName(), points, true,
						points < item.points() ? "Засчитано частично: максимум за ИД в этом вузе — " + limit : null));
			}
		}
		return result;
	}

	private String subjectName(String subjectId) {
		return subjectNames.getOrDefault(subjectId, subjectId);
	}

	/** Что известно об абитуриенте: баллы ЕГЭ по id предмета, дипломы олимпиад, отмеченные ИД. */
	public record Applicant(Map<String, Integer> egeScores, List<Diploma> diplomas, List<Achievement> achievements) {
	}

	public record Diploma(OlympiadProfile profile, OlympiadDegree degree) {
	}

	/**
	 * Что диплом даёт на направлении. points заполнен только для баллов за ИД.
	 * hundredPoints — диплом даёт 100 баллов в общем конкурсе, в том числе вместе с БВИ.
	 */
	public record DiplomaResult(Diploma diploma, OlympiadBenefit benefit, Integer points, boolean hundredPoints,
			String note) {
	}

	/**
	 * Предмет ЕГЭ направления.
	 *
	 * @param egeScore     результат ЕГЭ; null, если его нет
	 * @param score        балл с учётом олимпиады (100 вместо ЕГЭ); null, если его нет
	 * @param counted      предмет вошёл в сумму (из взаимозаменяемых засчитывается один)
	 * @param fromOlympiad вместо ЕГЭ подставлено 100 баллов по диплому
	 */
	public record ExamResult(String subjectId, String subjectName, Integer egeScore, Integer score, boolean counted,
			boolean fromOlympiad, String note) {
	}

	public record AchievementResult(String achievementId, String achievementName, int points, boolean counted,
			String note) {

		static AchievementResult notCounted(String achievementId, String achievementName, String note) {
			return new AchievementResult(achievementId, achievementName, 0, false, note);
		}
	}

	/**
	 * @param benefit          лучшая льгота на направлении: BVI, SCORE_100 или NONE
	 * @param egeScore         сумма ЕГЭ без льгот
	 * @param egeScoreWithBenefits сумма ЕГЭ, где по льготам подставлено 100
	 * @param missingSubjects  предметы направления, по которым нет результата ЕГЭ
	 * @param exams            каждый предмет направления: засчитан ли и почему
	 * @param totalScore       конкурсный балл общего конкурса: ЕГЭ с льготами + ИД
	 */
	public record ProgramResult(
			Program program,
			OlympiadBenefit benefit,
			List<DiplomaResult> diplomas,
			int egeScore,
			int egeScoreWithBenefits,
			List<String> missingSubjects,
			List<ExamResult> exams,
			List<AchievementResult> achievements,
			int achievementPoints,
			int totalScore) {
	}

	private record EgeSum(int total, List<String> missing, List<ExamResult> exams) {
	}
}
