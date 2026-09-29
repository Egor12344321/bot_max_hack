package com.runiversityadmisson.bot.application.benefit;

import com.runiversityadmisson.bot.application.dto.benefit.AchievementScoreResponse;
import com.runiversityadmisson.bot.application.dto.benefit.AdmissionBenefitsResponse;
import com.runiversityadmisson.bot.application.dto.benefit.OlympiadBenefitResponse;
import com.runiversityadmisson.bot.application.dto.benefit.ProgramBenefitsResponse;
import com.runiversityadmisson.bot.application.dto.benefit.UniversityBenefitsResponse;
import com.runiversityadmisson.bot.domain.applicant.model.exam.EgeScore;
import com.runiversityadmisson.bot.domain.applicant.model.olympiad.OlympiadBenefit;
import com.runiversityadmisson.bot.domain.applicant.model.olympiad.OlympiadBenefitRule;
import com.runiversityadmisson.bot.domain.applicant.model.olympiad.OlympiadDiploma;
import com.runiversityadmisson.bot.domain.applicant.model.olympiad.OlympiadProfile;
import com.runiversityadmisson.bot.domain.applicant.model.university.Program;
import com.runiversityadmisson.bot.domain.applicant.model.exam.Subject;
import com.runiversityadmisson.bot.domain.applicant.model.university.University;
import com.runiversityadmisson.bot.domain.applicant.model.profile.User;
import com.runiversityadmisson.bot.domain.applicant.ports.benefit.AchievementRepository;
import com.runiversityadmisson.bot.domain.applicant.ports.exam.EgeScoreRepository;
import com.runiversityadmisson.bot.domain.applicant.ports.olympiad.OlympiadBenefitRuleRepository;
import com.runiversityadmisson.bot.domain.applicant.ports.olympiad.OlympiadProfileRepository;
import com.runiversityadmisson.bot.domain.applicant.ports.university.ProgramRepository;
import com.runiversityadmisson.bot.domain.applicant.ports.exam.SubjectRepository;
import com.runiversityadmisson.bot.domain.applicant.ports.profile.UserRepository;
import com.runiversityadmisson.bot.domain.applicant.service.AdmissionBenefitCalculator;
import com.runiversityadmisson.bot.domain.applicant.service.AdmissionBenefitCalculator.Applicant;
import com.runiversityadmisson.bot.domain.applicant.service.AdmissionBenefitCalculator.Diploma;
import com.runiversityadmisson.bot.domain.applicant.service.AdmissionBenefitCalculator.DiplomaResult;
import com.runiversityadmisson.bot.domain.applicant.service.AdmissionBenefitCalculator.ProgramResult;
import com.runiversityadmisson.bot.presentation.exception.ResourceNotFoundException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Собирает данные абитуриента и правила вузов и считает льготы по каждому направлению. */
@Service
@RequiredArgsConstructor
public class AdmissionBenefitService {

	static final String BVI_NOTE = "БВИ можно использовать только в одном вузе и только на одном направлении. "
			+ "Выбери, где он выгоднее всего, а на остальные направления подавай на общих основаниях.";

	private final UserRepository userRepository;
	private final EgeScoreRepository egeScoreRepository;
	private final SubjectRepository subjectRepository;
	private final AchievementRepository achievementRepository;
	private final OlympiadProfileRepository olympiadProfileRepository;
	private final ProgramRepository programRepository;
	private final OlympiadBenefitRuleRepository olympiadBenefitRuleRepository;

	@Value("${admission.campaign-year:2026}")
	private int campaignYear;

	/**
	 * @param universityId если задан — только направления этого вуза
	 */
	@Transactional(readOnly = true)
	public AdmissionBenefitsResponse getBenefits(UUID sessionId, String universityId) {
		List<ProgramResult> results = calculate(sessionId, universityId);

		Map<String, List<ProgramResult>> byUniversity = results.stream()
				.collect(Collectors.groupingBy(result -> result.program().getUniversity().getId(), LinkedHashMap::new,
						Collectors.toList()));
		List<UniversityBenefitsResponse> universities = byUniversity.values().stream()
				.map(programs -> {
					University university = programs.getFirst().program().getUniversity();
					return new UniversityBenefitsResponse(university.getId(), university.getShortName(),
							university.getAchievementPointsMax(),
							programs.stream().map(AdmissionBenefitService::toResponse).toList());
				})
				.toList();
		boolean hasBvi = results.stream().anyMatch(result -> result.benefit() == OlympiadBenefit.BVI);
		return new AdmissionBenefitsResponse(campaignYear, universities, hasBvi ? BVI_NOTE : null);
	}

	/** На скольких направлениях дипломы пользователя дают БВИ. */
	@Transactional(readOnly = true)
	public long countBviPrograms(UUID sessionId) {
		return calculate(sessionId, null).stream()
				.filter(result -> result.benefit() == OlympiadBenefit.BVI)
				.count();
	}

	private List<ProgramResult> calculate(UUID sessionId, String universityId) {
		User user = userRepository.findById(sessionId)
				.orElseThrow(() -> new ResourceNotFoundException("Заявка не найдена"));
		List<Program> programs = universityId == null
				? programRepository.findAllByOrderByUniversityIdAscIdAsc()
				: programRepository.findByUniversityIdOrderByIdAsc(universityId);
		if (universityId != null && programs.isEmpty()) {
			throw new ResourceNotFoundException("Вуз не найден");
		}

		Map<String, Integer> egeScores = egeScoreRepository.findByUserId(sessionId).stream()
				.collect(Collectors.toMap(EgeScore::getSubjectId, EgeScore::getScore));
		Applicant applicant = new Applicant(egeScores, diplomas(user),
				achievementRepository.findAllById(user.getAchievementIds()));
		Map<String, String> subjectNames = subjectRepository.findAll().stream()
				.collect(Collectors.toMap(Subject::getId, Subject::getNameRu));

		AdmissionBenefitCalculator calculator = new AdmissionBenefitCalculator(campaignYear, subjectNames);
		List<OlympiadBenefitRule> rules = olympiadBenefitRuleRepository.findAll();
		return programs.stream()
				.map(program -> calculator.calculate(program, applicant, rules))
				.toList();
	}

	private List<Diploma> diplomas(User user) {
		Map<String, OlympiadProfile> profiles = olympiadProfileRepository
				.findAllById(user.getOlympiadDiplomas().stream().map(OlympiadDiploma::getProfileId).toList())
				.stream()
				.collect(Collectors.toMap(OlympiadProfile::getId, Function.identity()));
		return user.getOlympiadDiplomas().stream()
				.map(diploma -> new Diploma(profiles.get(diploma.getProfileId()), diploma.getDegree()))
				.toList();
	}

	private static ProgramBenefitsResponse toResponse(ProgramResult result) {
		Program program = result.program();
		return new ProgramBenefitsResponse(
				program.getId(),
				program.getName(),
				result.benefit().getCode(),
				result.egeScore(),
				result.egeScoreWithBenefits(),
				result.achievementPoints(),
				result.totalScore(),
				program.getPassingScorePreviousYear(),
				result.missingSubjects(),
				result.diplomas().stream().map(AdmissionBenefitService::toResponse).toList(),
				result.achievements().stream()
						.map(item -> new AchievementScoreResponse(item.achievementId(), item.achievementName(),
								item.points(), item.counted(), item.note()))
						.toList());
	}

	private static OlympiadBenefitResponse toResponse(DiplomaResult result) {
		OlympiadProfile profile = result.diploma().profile();
		return new OlympiadBenefitResponse(profile.getId(), profile.getOlympiad().getName(), profile.getName(),
				profile.getLevel(), result.diploma().degree().getCode(), result.benefit().getCode(), result.points(),
				result.note());
	}
}
