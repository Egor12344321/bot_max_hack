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

	public RecommendationService(UserRepository userRepository, StudyDirectionRepository directionRepository,
			ProgramRepository programRepository, SubjectRepository subjectRepository,
			AdmissionBenefitService admissionBenefitService,
			@Value("${admission.near-previous-threshold:5}") int nearPreviousThreshold) {
		this.userRepository = userRepository;
		this.directionRepository = directionRepository;
		this.programRepository = programRepository;
		this.subjectRepository = subjectRepository;
		this.admissionBenefitService = admissionBenefitService;
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

		List<Program> programs = programRepository.findByDirectionIdOrderByUniversityIdAscIdAsc(directionId);
		Map<String, Integer> minScores = subjectRepository.findAll().stream()
				.collect(Collectors.toMap(Subject::getId, Subject::getMinThreshold));
		StudyDirectionResponse directionResponse =
				new StudyDirectionResponse(direction.getId(), direction.getCode(), direction.getName());

		List<Evaluated> evaluated = admissionBenefitService.calculateForPrograms(sessionId, programs).stream()
				.map(result -> evaluate(result, minScores))
				.toList();
		List<ProgramOptionResponse> selected = policy.select(evaluated, Evaluated::candidate).stream()
				.map(item -> toResponse(item, directionResponse))
				.toList();
		return new ProgramOptionPageResponse(selected.stream().skip(offset).limit(limit).toList(), selected.size());
	}

	private Evaluated evaluate(ProgramResult result, Map<String, Integer> minScores) {
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
		Integer passing = result.program().getPassingScorePreviousYear();
		Integer difference = eligibility == Eligibility.ELIGIBLE && passing != null ? totalScore - passing : null;
		Candidate candidate = new Candidate(result.program().getUniversity().getId(), result.program().getId(),
				result.benefit() == OlympiadBenefit.BVI, eligibility, totalScore, difference);
		return new Evaluated(result, candidate, belowMinimum);
	}

	private ProgramOptionResponse toResponse(Evaluated item, StudyDirectionResponse direction) {
		ProgramResult result = item.result();
		Program program = result.program();
		Candidate candidate = item.candidate();
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
				"general",
				candidate.eligibility().getCode(),
				candidate.bviAvailable(),
				candidate.totalScore(),
				program.getPassingScorePreviousYear(),
				program.getPassingScoreYear(),
				candidate.scoreDifference(),
				policy.compare(candidate).getCode(),
				"demo",
				breakdown(result),
				reasons(item));
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
		result.missingSubjects().forEach(subject -> reasons.add("Нет результата ЕГЭ: " + subject));
		reasons.addAll(item.belowMinimum());
		if (result.program().getPassingScoreNote() != null) {
			reasons.add(result.program().getPassingScoreNote());
		}
		if (item.candidate().bviAvailable()) {
			reasons.add(BVI_REMINDER);
		}
		result.diplomas().stream()
				.filter(diploma -> diploma.benefit() == OlympiadBenefit.NONE && diploma.note().startsWith("Для льготы"))
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

	private record Evaluated(ProgramResult result, Candidate candidate, List<String> belowMinimum) {
	}
}
