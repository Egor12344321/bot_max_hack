package com.runiversityadmisson.bot.domain.applicant.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.runiversityadmisson.bot.domain.applicant.model.Achievement;
import com.runiversityadmisson.bot.domain.applicant.model.Olympiad;
import com.runiversityadmisson.bot.domain.applicant.model.OlympiadBenefit;
import com.runiversityadmisson.bot.domain.applicant.model.OlympiadBenefitRule;
import com.runiversityadmisson.bot.domain.applicant.model.OlympiadDegree;
import com.runiversityadmisson.bot.domain.applicant.model.OlympiadProfile;
import com.runiversityadmisson.bot.domain.applicant.model.Program;
import com.runiversityadmisson.bot.domain.applicant.model.ProgramSubject;
import com.runiversityadmisson.bot.domain.applicant.model.University;
import com.runiversityadmisson.bot.domain.applicant.service.AdmissionBenefitCalculator.AchievementResult;
import com.runiversityadmisson.bot.domain.applicant.service.AdmissionBenefitCalculator.Applicant;
import com.runiversityadmisson.bot.domain.applicant.service.AdmissionBenefitCalculator.Diploma;
import com.runiversityadmisson.bot.domain.applicant.service.AdmissionBenefitCalculator.DiplomaResult;
import com.runiversityadmisson.bot.domain.applicant.service.AdmissionBenefitCalculator.ProgramResult;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AdmissionBenefitCalculatorTest {

	private static final Map<String, String> SUBJECT_NAMES = Map.of(
			"math-profile", "Математика (профиль)",
			"informatics", "Информатика",
			"physics", "Физика",
			"russian", "Русский язык");

	private final AdmissionBenefitCalculator calculator = new AdmissionBenefitCalculator(2026, SUBJECT_NAMES);

	private final University university = university("hse", 10, Map.of());
	private final Program program = program(university, "hse-software", Set.of("math", "informatics"),
			subject("math-profile", null), subject("informatics", null), subject("russian", null));

	private final Olympiad highSchoolTest = olympiad("vysshaya-proba", false);
	private final OlympiadProfile mathLevel1 = profile(highSchoolTest, "math", "math-profile", 1);

	@Test
	void winnerGetsBviWhenEgeConfirmsDiploma() {
		ProgramResult result = calculate(program, applicant(Map.of("math-profile", 80), diploma(mathLevel1, OlympiadDegree.WINNER)),
				rule("hse", null, "math", 2, OlympiadDegree.WINNER, OlympiadBenefit.BVI, 75),
				rule("hse", null, "math", 3, null, OlympiadBenefit.SCORE_100, 75));

		assertThat(result.benefit()).isEqualTo(OlympiadBenefit.BVI);
		assertThat(result.diplomas().getFirst().note()).isEqualTo("Зачисление без вступительных испытаний");
	}

	@Test
	void prizeWinnerGetsHundredPointsInsteadOfEge() {
		ProgramResult result = calculate(program,
				applicant(Map.of("math-profile", 80, "informatics", 70, "russian", 90), diploma(mathLevel1, OlympiadDegree.PRIZE)),
				rule("hse", null, "math", 2, OlympiadDegree.WINNER, OlympiadBenefit.BVI, 75),
				rule("hse", null, "math", 3, null, OlympiadBenefit.SCORE_100, 75));

		assertThat(result.benefit()).isEqualTo(OlympiadBenefit.SCORE_100);
		assertThat(result.egeScore()).isEqualTo(240);
		assertThat(result.egeScoreWithBenefits()).isEqualTo(260);
		assertThat(result.totalScore()).isEqualTo(260);
		assertThat(result.diplomas().getFirst().note()).isEqualTo("100 баллов по предмету «Математика (профиль)»");
	}

	@Test
	void fallsBackToHundredPointsWhenEgeIsTooLowForBvi() {
		ProgramResult result = calculate(program, applicant(Map.of("math-profile", 80), diploma(mathLevel1, OlympiadDegree.WINNER)),
				rule("hse", null, "math", 1, null, OlympiadBenefit.BVI, 85),
				rule("hse", null, "math", 2, null, OlympiadBenefit.SCORE_100, 75));

		assertThat(result.benefit()).isEqualTo(OlympiadBenefit.SCORE_100);
		assertThat(result.diplomas().getFirst().note()).isEqualTo("100 баллов по предмету «Математика (профиль)». "
				+ "Для льготы «БВИ» нужен ЕГЭ по предмету «Математика (профиль)» не ниже 85, сейчас 80");
	}

	@Test
	void givesNothingWhenEgeDoesNotConfirmDiploma() {
		ProgramResult result = calculate(program, applicant(Map.of("math-profile", 70), diploma(mathLevel1, OlympiadDegree.WINNER)),
				rule("hse", null, "math", 1, null, OlympiadBenefit.BVI, 85),
				rule("hse", null, "math", 2, null, OlympiadBenefit.SCORE_100, 75));

		assertThat(result.benefit()).isEqualTo(OlympiadBenefit.NONE);
		assertThat(result.diplomas().getFirst().note())
				.isEqualTo("Для льготы «100 баллов» нужен ЕГЭ по предмету «Математика (профиль)» не ниже 75, сейчас 70");
	}

	@Test
	void explainsMissingEgeResult() {
		ProgramResult result = calculate(program, applicant(Map.of(), diploma(mathLevel1, OlympiadDegree.WINNER)),
				rule("hse", null, "math", 2, null, OlympiadBenefit.SCORE_100, 75));

		assertThat(result.diplomas().getFirst().note()).endsWith("не ниже 75, результата пока нет");
		assertThat(result.missingSubjects()).containsExactly("Математика (профиль)", "Информатика", "Русский язык");
	}

	@Test
	void ignoresOlympiadOfLowerLevel() {
		OlympiadProfile mathLevel3 = profile(olympiad("belchonok", false), "math", "math-profile", 3);

		ProgramResult result = calculate(program, applicant(Map.of("math-profile", 90), diploma(mathLevel3, OlympiadDegree.WINNER)),
				rule("hse", null, "math", 2, null, OlympiadBenefit.SCORE_100, 75));

		assertThat(result.benefit()).isEqualTo(OlympiadBenefit.NONE);
		assertThat(result.diplomas().getFirst().note()).isEqualTo("Не даёт льгот на этом направлении");
	}

	@Test
	void ignoresProfileThatDoesNotMatchProgram() {
		OlympiadProfile physics = profile(highSchoolTest, "physics", "physics", 1);

		ProgramResult result = calculate(program, applicant(Map.of("physics", 95), diploma(physics, OlympiadDegree.WINNER)),
				rule("hse", null, "physics", 3, null, OlympiadBenefit.BVI, 75));

		assertThat(result.benefit()).isEqualTo(OlympiadBenefit.NONE);
	}

	@Test
	void rulesOfOtherUniversityOrProgramDoNotApply() {
		ProgramResult result = calculate(program, applicant(Map.of("math-profile", 95), diploma(mathLevel1, OlympiadDegree.WINNER)),
				rule("msu", null, "math", 3, null, OlympiadBenefit.BVI, 75),
				rule("hse", "hse-data", "math", 3, null, OlympiadBenefit.BVI, 75));

		assertThat(result.benefit()).isEqualTo(OlympiadBenefit.NONE);
	}

	@Test
	void vsoshGivesBviInAnyUniversityWithoutEge() {
		OlympiadProfile vsoshInformatics = profile(olympiad("vsosh", true), "informatics", "informatics", null);
		OlympiadBenefitRule vsoshRule = rule(null, null, "informatics", null, null, OlympiadBenefit.BVI, null);
		vsoshRule.setVsosh(true);

		ProgramResult result = calculate(program, applicant(Map.of(), diploma(vsoshInformatics, OlympiadDegree.PRIZE)), vsoshRule);

		assertThat(result.benefit()).isEqualTo(OlympiadBenefit.BVI);
	}

	@Test
	void vsoshRuleDoesNotApplyToListOlympiads() {
		OlympiadBenefitRule vsoshRule = rule(null, null, "math", null, null, OlympiadBenefit.BVI, null);
		vsoshRule.setVsosh(true);

		ProgramResult result = calculate(program, applicant(Map.of("math-profile", 100), diploma(mathLevel1, OlympiadDegree.WINNER)),
				vsoshRule);

		assertThat(result.benefit()).isEqualTo(OlympiadBenefit.NONE);
	}

	@Test
	void olympiadSpecificRuleAppliesOnlyToThatOlympiad() {
		OlympiadProfile phystechMath = profile(olympiad("phystech", false), "math", "math-profile", 2);
		OlympiadProfile rosatomMath = profile(olympiad("rosatom", false), "math", "math-profile", 2);
		OlympiadBenefitRule phystechOnly = rule("hse", null, "math", 2, null, OlympiadBenefit.BVI, 75);
		phystechOnly.setOlympiadId("phystech");

		ProgramResult phystech = calculate(program, applicant(Map.of("math-profile", 90), diploma(phystechMath, OlympiadDegree.PRIZE)),
				phystechOnly);
		ProgramResult rosatom = calculate(program, applicant(Map.of("math-profile", 90), diploma(rosatomMath, OlympiadDegree.PRIZE)),
				phystechOnly);

		assertThat(phystech.benefit()).isEqualTo(OlympiadBenefit.BVI);
		assertThat(rosatom.benefit()).isEqualTo(OlympiadBenefit.NONE);
	}

	@Test
	void takesBestAlternativeSubjectIncludingHundredPoints() {
		Program itmo = program(university("itmo", 10, Map.of()), "itmo-software", Set.of("math", "informatics", "physics"),
				subject("math-profile", null), subject("russian", null),
				subject("informatics", 1), subject("physics", 1));
		OlympiadProfile physics = profile(highSchoolTest, "physics", "physics", 2);

		ProgramResult result = calculate(itmo,
				applicant(Map.of("math-profile", 80, "russian", 90, "informatics", 88, "physics", 76), diploma(physics, OlympiadDegree.PRIZE)),
				rule("itmo", null, "physics", 3, null, OlympiadBenefit.SCORE_100, 75));

		assertThat(result.egeScore()).isEqualTo(80 + 90 + 88);
		assertThat(result.egeScoreWithBenefits()).isEqualTo(80 + 90 + 100);
	}

	@Test
	void reportsMissingChoiceGroupAsAlternatives() {
		Program itmo = program(university("itmo", 10, Map.of()), "itmo-software", Set.of(),
				subject("math-profile", null), subject("russian", null),
				subject("informatics", 1), subject("physics", 1));

		ProgramResult result = calculate(itmo, applicant(Map.of("math-profile", 80, "russian", 90)));

		assertThat(result.missingSubjects()).containsExactly("Информатика или Физика");
		assertThat(result.egeScore()).isEqualTo(170);
	}

	@Test
	void capsAchievementPointsAtUniversityLimit() {
		University hse = university("hse", 10, Map.of("medal_gold", 6, "gto_gold", 3, "volunteering", 2));
		Program hseProgram = program(hse, "hse-software", Set.of(), subject("russian", null));
		Applicant applicant = new Applicant(Map.of("russian", 90), List.of(), List.of(
				achievement("volunteering"), achievement("essay"), achievement("gto_gold"), achievement("medal_gold")));

		ProgramResult result = calculator.calculate(hseProgram, applicant, List.of());

		assertThat(result.achievementPoints()).isEqualTo(10);
		assertThat(result.totalScore()).isEqualTo(100);
		assertThat(result.achievements()).containsExactly(
				new AchievementResult("medal_gold", "medal_gold", 6, true, null),
				new AchievementResult("gto_gold", "gto_gold", 3, true, null),
				new AchievementResult("volunteering", "volunteering", 1, true,
						"Засчитано частично: максимум за ИД в этом вузе — 10"),
				new AchievementResult("essay", "essay", 0, false, "Не учитывается в этом вузе"));
	}

	@Test
	void achievementsOverLimitAreNotCounted() {
		University msu = university("msu", 6, Map.of("medal_gold", 6, "gto_gold", 2));
		Program msuProgram = program(msu, "msu-math", Set.of());
		Applicant applicant = new Applicant(Map.of(), List.of(), List.of(achievement("gto_gold"), achievement("medal_gold")));

		ProgramResult result = calculator.calculate(msuProgram, applicant, List.of());

		assertThat(result.achievements()).extracting(AchievementResult::counted).containsExactly(true, false);
		assertThat(result.achievements().get(1).note()).isEqualTo("Уже набран максимум за ИД в этом вузе (6)");
	}

	@Test
	void countsOnlyBestOlympiadAsAchievement() {
		OlympiadProfile belchonokMath = profile(olympiad("belchonok", false), "math", "math-profile", 3);
		OlympiadProfile futureMath = profile(olympiad("step-into-future", false), "math", "math-profile", 3);
		OlympiadBenefitRule fivePoints = rule("hse", null, "math", 3, null, OlympiadBenefit.ACHIEVEMENT_POINTS, null);
		fivePoints.setPoints(5);

		ProgramResult result = calculate(program,
				applicant(Map.of(), diploma(belchonokMath, OlympiadDegree.PRIZE), diploma(futureMath, OlympiadDegree.WINNER)),
				fivePoints);

		assertThat(result.benefit()).isEqualTo(OlympiadBenefit.NONE);
		assertThat(result.achievementPoints()).isEqualTo(5);
		assertThat(result.achievements()).extracting(AchievementResult::counted).containsExactly(true, false);
		assertThat(result.achievements().get(1).note()).isEqualTo("Засчитывается только одна олимпиада");
	}

	@Test
	void expiredDiplomaGivesNothing() {
		OlympiadProfile oldMath = profile(highSchoolTest, "math", "math-profile", 1);
		oldMath.setOlympiadYear(2021);

		ProgramResult result = calculate(program, applicant(Map.of("math-profile", 100), diploma(oldMath, OlympiadDegree.WINNER)),
				rule("hse", null, "math", 3, null, OlympiadBenefit.BVI, 75));

		DiplomaResult diploma = result.diplomas().getFirst();
		assertThat(diploma.benefit()).isEqualTo(OlympiadBenefit.NONE);
		assertThat(diploma.note()).isEqualTo("Диплом 2021 года не действует в приёмной кампании 2026 года");
	}

	@Test
	void diplomaIsValidForFourYears() {
		OlympiadProfile math2022 = profile(highSchoolTest, "math", "math-profile", 1);
		math2022.setOlympiadYear(2022);

		ProgramResult result = calculate(program, applicant(Map.of("math-profile", 100), diploma(math2022, OlympiadDegree.WINNER)),
				rule("hse", null, "math", 3, null, OlympiadBenefit.BVI, 75));

		assertThat(result.benefit()).isEqualTo(OlympiadBenefit.BVI);
	}

	private ProgramResult calculate(Program target, Applicant applicant, OlympiadBenefitRule... rules) {
		return calculator.calculate(target, applicant, List.of(rules));
	}

	private static Applicant applicant(Map<String, Integer> egeScores, Diploma... diplomas) {
		return new Applicant(egeScores, List.of(diplomas), List.of());
	}

	private static Diploma diploma(OlympiadProfile profile, OlympiadDegree degree) {
		return new Diploma(profile, degree);
	}

	private static University university(String id, int achievementPointsMax, Map<String, Integer> achievementPoints) {
		University university = new University();
		university.setId(id);
		university.setShortName(id);
		university.setAchievementPointsMax(achievementPointsMax);
		university.setAchievementPoints(achievementPoints);
		return university;
	}

	private static Program program(University university, String id, Set<String> olympiadProfiles,
			ProgramSubject... subjects) {
		Program program = new Program();
		program.setId(id);
		program.setName(id);
		program.setUniversity(university);
		program.setOlympiadProfiles(olympiadProfiles);
		program.setSubjects(List.of(subjects));
		return program;
	}

	private static ProgramSubject subject(String subjectId, Integer choiceGroup) {
		return new ProgramSubject(subjectId, choiceGroup);
	}

	private static Olympiad olympiad(String id, boolean vsosh) {
		Olympiad olympiad = new Olympiad();
		olympiad.setId(id);
		olympiad.setName(id);
		olympiad.setVsosh(vsosh);
		return olympiad;
	}

	private static OlympiadProfile profile(Olympiad olympiad, String profileCode, String subjectId, Integer level) {
		OlympiadProfile profile = new OlympiadProfile();
		profile.setId(olympiad.getId() + "-" + profileCode);
		profile.setOlympiad(olympiad);
		profile.setProfile(profileCode);
		profile.setName(profileCode);
		profile.setSubjectId(subjectId);
		profile.setLevel(level);
		profile.setOlympiadYear(2026);
		return profile;
	}

	private static OlympiadBenefitRule rule(String universityId, String programId, String profile, Integer maxLevel,
			OlympiadDegree degree, OlympiadBenefit benefit, Integer minEgeScore) {
		OlympiadBenefitRule rule = new OlympiadBenefitRule();
		rule.setUniversityId(universityId);
		rule.setProgramId(programId);
		rule.setProfile(profile);
		rule.setMaxLevel(maxLevel);
		rule.setDegree(degree);
		rule.setBenefit(benefit);
		rule.setMinEgeScore(minEgeScore);
		return rule;
	}

	private static Achievement achievement(String id) {
		Achievement achievement = new Achievement();
		achievement.setId(id);
		achievement.setName(id);
		return achievement;
	}
}
