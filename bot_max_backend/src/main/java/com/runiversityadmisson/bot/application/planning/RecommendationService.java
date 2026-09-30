package com.runiversityadmisson.bot.application.planning;

import com.runiversityadmisson.bot.application.benefit.AdmissionBenefitService;
import com.runiversityadmisson.bot.application.dto.direction.StudyDirectionResponse;
import com.runiversityadmisson.bot.application.dto.planning.ProgramBreakdownItemResponse;
import com.runiversityadmisson.bot.application.dto.planning.ProgramOptionPageResponse;
import com.runiversityadmisson.bot.application.dto.planning.ProgramOptionResponse;
import com.runiversityadmisson.bot.domain.applicant.model.direction.StudyDirection;
import com.runiversityadmisson.bot.domain.applicant.model.exam.Subject;
import com.runiversityadmisson.bot.domain.applicant.model.olympiad.OlympiadBenefit;
import com.runiversityadmisson.bot.domain.applicant.model.olympiad.OlympiadDegree;
import com.runiversityadmisson.bot.domain.applicant.model.olympiad.OlympiadProfile;
import com.runiversityadmisson.bot.domain.applicant.model.profile.User;
import com.runiversityadmisson.bot.domain.applicant.model.university.Program;
import com.runiversityadmisson.bot.domain.applicant.ports.direction.StudyDirectionRepository;
import com.runiversityadmisson.bot.domain.applicant.ports.exam.SubjectRepository;
import com.runiversityadmisson.bot.domain.applicant.ports.profile.UserRepository;
import com.runiversityadmisson.bot.domain.applicant.ports.university.ProgramRepository;
import com.runiversityadmisson.bot.domain.applicant.service.AdmissionBenefitCalculator.AchievementResult;
import com.runiversityadmisson.bot.domain.applicant.service.AdmissionBenefitCalculator.DiplomaResult;
import com.runiversityadmisson.bot.domain.applicant.service.AdmissionBenefitCalculator.ExamResult;
import com.runiversityadmisson.bot.domain.applicant.service.AdmissionBenefitCalculator.ProgramResult;
import com.runiversityadmisson.bot.domain.applicant.service.ProgramRecommendationPolicy;
import com.runiversityadmisson.bot.domain.applicant.service.ProgramRecommendationPolicy.Candidate;
import com.runiversityadmisson.bot.domain.applicant.service.ProgramRecommendationPolicy.Eligibility;
import com.runiversityadmisson.bot.presentation.exception.BadRequestException;
import com.runiversityadmisson.bot.presentation.exception.ResourceNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * «Рекомендации по направлению»: программы выбранного направления во всех вузах
 * с персональным расчётом, отобранные {@link ProgramRecommendationPolicy}.
 * Все программы демо-БД — бюджет, очная форма, общий конкурс.
 */
@Service
public class RecommendationService {

	static final String BVI_REMINDER = "БВИ можно использовать только в одном вузе и только на одном направлении";

	private final UserRepository userRepository;
	private final StudyDirectionRepository directionRepository;
	private final ProgramRepository programRepository;
	private final SubjectRepository subjectRepository;
	private final AdmissionBenefitService admissionBenefitService;
	private final ProgramRecommendationPolicy policy;
	private final CompetitionService competitions;

	public RecommendationService(UserRepository userRepository, StudyDirectionRepository directionRepository,
			ProgramRepository programRepository, SubjectRepository subjectRepository,
			AdmissionBenefitService admissionBenefitService,
			CompetitionService competitions,
			@Value("${admission.near-previous-threshold:5}") int nearPreviousThreshold) {
		this.userRepository = userRepository;
		this.directionRepository = directionRepository;
		this.programRepository = programRepository;
		this.subjectRepository = subjectRepository;
		this.admissionBenefitService = admissionBenefitService;
		this.competitions = competitions;
		this.policy = new ProgramRecommendationPolicy(nearPreviousThreshold);
	}

	@Transactional(readOnly = true)
	public ProgramOptionPageResponse getRecommendations(UUID sessionId, String directionId, int offset, int limit) {
		if (offset < 0 || limit < 1 || limit > 100) {
			throw new BadRequestException("offset должен быть >= 0, limit — от 1 до 100");
		}
		if (directionId == null || directionId.isBlank()) {
			throw new BadRequestException("Укажите directionId");
		}
		User user = userRepository.findById(sessionId)
				.orElseThrow(() -> new ResourceNotFoundException("Заявка не найдена"));
		StudyDirection direction = directionRepository.findById(directionId)
				.orElseThrow(() -> new ResourceNotFoundException("Направление не найдено"));
		if (!user.getDirectionIds().contains(directionId)) {
			throw new BadRequestException("Направление не выбрано");
		}

		List<ProgramOptionResponse> selected = recommend(sessionId, direction, minScores());
		return new ProgramOptionPageResponse(selected.stream().skip(offset).limit(limit).toList(), selected.size());
	}

	/** Подборки по всем выбранным направлениям пользователя, в порядке его выбора. */
	@Transactional(readOnly = true)
	public List<ProgramOptionResponse> getAllRecommendations(UUID sessionId) {
		User user = userRepository.findById(sessionId)
				.orElseThrow(() -> new ResourceNotFoundException("Заявка не найдена"));
		Map<String, StudyDirection> directions = directionRepository.findAllById(user.getDirectionIds()).stream()
				.collect(Collectors.toMap(StudyDirection::getId, direction -> direction));
		Map<String, Integer> minScores = minScores();
		return user.getDirectionIds().stream()
				.filter(directions::containsKey)
				.flatMap(id -> recommend(sessionId, directions.get(id), minScores).stream())
				.toList();
	}

	/**
	 * Персональный расчёт переданных программ без отбора и лимита: для автоплана нужен
	 * полный набор кандидатов, а не первая страница рекомендаций. Порядок как у programs.
	 */
	@Transactional(readOnly = true)
	public List<ProgramOptionResponse> evaluatePrograms(UUID sessionId, List<Program> programs) {
		User user = userRepository.findById(sessionId).orElseThrow(() -> new ResourceNotFoundException("Заявка не найдена"));
		List<String> types = competitions.available(user);
		Map<String, StudyDirectionResponse> directions = directionRepository
				.findAllById(programs.stream().map(Program::getDirectionId).distinct().toList())
				.stream()
				.collect(Collectors.toMap(StudyDirection::getId,
						direction -> new StudyDirectionResponse(direction.getId(), direction.getCode(), direction.getName())));
		Map<String, Integer> minScores = minScores();
		return admissionBenefitService.calculateForPrograms(sessionId, programs).stream()
				.map(result -> toResponse(evaluate(result, minScores, types), directions.get(result.program().getDirectionId())))
				.toList();
	}

	private List<ProgramOptionResponse> recommend(UUID sessionId, StudyDirection direction,
			Map<String, Integer> minScores) {
		List<Program> programs = programRepository.findByDirectionIdOrderByUniversityIdAscIdAsc(direction.getId());
		User user = userRepository.findById(sessionId).orElseThrow(() -> new ResourceNotFoundException("Заявка не найдена"));
		List<String> types = competitions.available(user);
		StudyDirectionResponse directionResponse =
				new StudyDirectionResponse(direction.getId(), direction.getCode(), direction.getName());
		List<Evaluated> evaluated = admissionBenefitService.calculateForPrograms(sessionId, programs).stream()
				.map(result -> evaluate(result, minScores, types))
				.toList();
		return policy.select(evaluated, Evaluated::candidate, user.getMaxScoreDeficit()).stream()
				.limit(10)
				.map(item -> toResponse(item, directionResponse))
				.toList();
	}

	private Map<String, Integer> minScores() {
		return subjectRepository.findAll().stream()
				.collect(Collectors.toMap(Subject::getId, Subject::getMinThreshold));
	}

	private Evaluated evaluate(ProgramResult result, Map<String, Integer> minScores, List<String> types) {
		List<String> belowMinimum = result.exams().stream()
				.filter(exam -> exam.counted() && !exam.fromOlympiad())
				.filter(exam -> exam.egeScore() < minScores.getOrDefault(exam.subjectId(), 0))
				.map(exam -> "ЕГЭ по предмету «" + exam.subjectName() + "» ниже минимального порога "
						+ minScores.get(exam.subjectId()))
				.toList();
		Eligibility eligibility = !result.missingSubjects().isEmpty() ? Eligibility.INCOMPLETE
				: !belowMinimum.isEmpty() ? Eligibility.INELIGIBLE
				: Eligibility.ELIGIBLE;
		Integer totalScore = eligibility == Eligibility.INCOMPLETE ? null : result.totalScore();
		String type = types.getFirst();
		var competition = result.program().getCompetitions().get(type);
		if (competition != null && Integer.valueOf(0).equals(competition.getSeats())) eligibility = Eligibility.INELIGIBLE;
		Integer passing = "general".equals(type) ? result.program().getPassingScorePreviousYear()
				: competition == null ? null : competition.getPassingScore();
		Integer difference = eligibility == Eligibility.ELIGIBLE && passing != null ? totalScore - passing : null;
		Candidate candidate = new Candidate(result.program().getUniversity().getId(), result.program().getId(),
				result.benefit() == OlympiadBenefit.BVI, eligibility, totalScore, difference, passing, entranceScoreMax(result.program()));
		return new Evaluated(result, candidate, belowMinimum, types);
	}

	private ProgramOptionResponse toResponse(Evaluated item, StudyDirectionResponse direction) {
		ProgramResult result = item.result();
		Program program = result.program();
		Candidate candidate = item.candidate();
		String type = item.types().getFirst();
		var competition = program.getCompetitions().get(type);
		return new ProgramOptionResponse(
				program.getId(),
				program.getName(),
				program.getUniversity().getId(),
				program.getUniversity().getShortName(),
				direction,
				program.getUniversity().getCity(),
				null,
				admissionBenefitService.getCampaignYear(),
				"budget",
				"full_time",
				type,
				candidate.eligibility().getCode(),
				candidate.bviAvailable(),
				candidate.totalScore(),
				candidate.passingScore(),
				"general".equals(type) ? program.getPassingScoreYear() : competition == null ? null : competition.getPreviousYear(),
				candidate.scoreDifference(),
				policy.compare(candidate).getCode(),
				competition == null ? "demo" : competition.getDataSource(),
				breakdown(result),
				reasons(item), competition == null ? null : competition.getSeats(), entranceScoreMax(program), item.types());
	}

	private static int entranceScoreMax(Program program) {
		return (int) program.getSubjects().stream().map(subject -> subject.getChoiceGroup() == null
				? "subject:" + subject.getSubjectId() : "group:" + subject.getChoiceGroup()).distinct().count() * 100;
	}

	private static List<ProgramBreakdownItemResponse> breakdown(ProgramResult result) {
		List<ProgramBreakdownItemResponse> items = new ArrayList<>();
		for (ExamResult exam : result.exams()) {
			items.add(new ProgramBreakdownItemResponse("exam", exam.subjectName(), exam.egeScore(),
					exam.counted() ? exam.score() : null, exam.counted(), exam.note()));
		}
		for (DiplomaResult diploma : result.diplomas()) {
			items.add(new ProgramBreakdownItemResponse("olympiad", diplomaName(diploma), null,
					diplomaScore(diploma), diploma.benefit() != OlympiadBenefit.NONE, diploma.note()));
		}
		for (AchievementResult achievement : result.achievements()) {
			items.add(new ProgramBreakdownItemResponse("achievement", achievement.achievementName(), null,
					achievement.counted() ? achievement.points() : null, achievement.counted(),
					achievement.note() == null ? "Баллы за индивидуальное достижение" : achievement.note()));
		}
		return items;
	}

	private static List<String> reasons(Evaluated item) {
		ProgramResult result = item.result();
		List<String> reasons = new ArrayList<>();
		String type = item.types().getFirst();
		var competition = result.program().getCompetitions().get(type);
		if (competition == null || competition.getSeats() == null) reasons.add("Число мест по этому конкурсу пока не загружено");
		if (!"general".equals(type)) {
			reasons.add("Выбрана квота по заявленной льготе. Право и документы подтверждает приёмная комиссия; общий конкурс также доступен.");
			if (item.candidate().passingScore() == null) reasons.add("Проходной по квоте неизвестен; проходной общего конкурса не используется");
			reasons.add("Освобождение от испытаний по отдельным основаниям не назначается автоматически; требуется проверка основания");
		}
		result.missingSubjects().forEach(subject -> reasons.add("Нет результата ЕГЭ: " + subject));
		reasons.addAll(item.belowMinimum());
		if ("general".equals(type) && result.program().getPassingScoreNote() != null) {
			reasons.add(result.program().getPassingScoreNote());
		}
		if (item.candidate().bviAvailable()) {
			reasons.add(BVI_REMINDER);
		}
		result.diplomas().stream()
				.filter(diploma -> diploma.benefit() == OlympiadBenefit.NONE && diploma.note() != null && diploma.note().startsWith("Для льготы"))
				.forEach(diploma -> reasons.add(diplomaName(diploma) + ": " + diploma.note()));
		return reasons;
	}

	private static String diplomaName(DiplomaResult diploma) {
		OlympiadProfile profile = diploma.diploma().profile();
		String level = profile.getLevel() == null ? "ВсОШ" : profile.getLevel() + " уровень";
		String degree = diploma.diploma().degree() == OlympiadDegree.WINNER ? "победитель" : "призёр";
		return profile.getOlympiad().getName() + " (" + profile.getName() + ", " + level + ", " + degree + ")";
	}

	private static Integer diplomaScore(DiplomaResult diploma) {
		if (diploma.benefit() == OlympiadBenefit.ACHIEVEMENT_POINTS) {
			return diploma.points();
		}
		return diploma.hundredPoints() ? Integer.valueOf(100) : null;
	}

	private record Evaluated(ProgramResult result, Candidate candidate, List<String> belowMinimum, List<String> types) {
	}
}
