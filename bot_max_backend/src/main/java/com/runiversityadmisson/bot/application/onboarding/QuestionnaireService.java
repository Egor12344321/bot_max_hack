package com.runiversityadmisson.bot.application.onboarding;

import com.runiversityadmisson.bot.application.dto.exam.EgeScoreInput;
import com.runiversityadmisson.bot.application.dto.profile.CitizenshipOptionResponse;
import com.runiversityadmisson.bot.application.dto.profile.CitizenshipResultResponse;
import com.runiversityadmisson.bot.application.dto.exam.EgeScoreResponse;
import com.runiversityadmisson.bot.application.dto.exam.EgeScoresSubmissionResponse;
import com.runiversityadmisson.bot.application.dto.direction.InterestCategoryResponse;
import com.runiversityadmisson.bot.application.dto.profile.LanguageResponse;
import com.runiversityadmisson.bot.application.dto.session.SessionResponse;
import com.runiversityadmisson.bot.application.dto.exam.SubjectResponse;
import com.runiversityadmisson.bot.domain.applicant.model.profile.CitizenshipOption;
import com.runiversityadmisson.bot.domain.applicant.model.exam.EgeScore;
import com.runiversityadmisson.bot.domain.applicant.model.direction.InterestCategory;
import com.runiversityadmisson.bot.domain.applicant.model.exam.Subject;
import com.runiversityadmisson.bot.domain.applicant.model.profile.User;
import com.runiversityadmisson.bot.domain.applicant.ports.profile.CitizenshipOptionRepository;
import com.runiversityadmisson.bot.domain.applicant.ports.exam.EgeScoreRepository;
import com.runiversityadmisson.bot.domain.applicant.ports.direction.InterestCategoryRepository;
import com.runiversityadmisson.bot.domain.applicant.ports.profile.LanguageRepository;
import com.runiversityadmisson.bot.domain.applicant.ports.exam.SubjectRepository;
import com.runiversityadmisson.bot.domain.applicant.ports.profile.UserRepository;
import com.runiversityadmisson.bot.presentation.exception.BadRequestException;
import com.runiversityadmisson.bot.presentation.exception.ResourceNotFoundException;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class QuestionnaireService {

	private final UserRepository userRepository;
	private final LanguageRepository languageRepository;
	private final CitizenshipOptionRepository citizenshipOptionRepository;
	private final SubjectRepository subjectRepository;
	private final EgeScoreRepository egeScoreRepository;
	private final InterestCategoryRepository interestCategoryRepository;

	@Transactional(readOnly = true)
	public List<LanguageResponse> getLanguages() {
		return languageRepository.findAll().stream()
				.sorted(Comparator.comparing(language -> language.getCode()))
				.map(language -> new LanguageResponse(language.getCode(), language.getName()))
				.toList();
	}

	@Transactional(readOnly = true)
	public List<CitizenshipOptionResponse> getCitizenshipOptions(UUID userId) {
		String language = getUser(userId).getLanguage();
		return citizenshipOptionRepository.findAll().stream()
				.sorted(Comparator.comparing(CitizenshipOption::getCode))
				.map(option -> new CitizenshipOptionResponse(option.getCode(), localizedName(option, language), option.getGroupName()))
				.toList();
	}

	@Transactional(readOnly = true)
	public List<SubjectResponse> getSubjects(UUID userId) {
		String language = getUser(userId).getLanguage();
		return subjectRepository.findAll().stream()
				.sorted(Comparator.comparing(Subject::getId))
				.map(subject -> new SubjectResponse(subject.getId(), localizedName(subject, language), subject.getMinThreshold()))
				.toList();
	}

	@Transactional(readOnly = true)
	public List<InterestCategoryResponse> getInterestCategories() {
		return interestCategoryRepository.findAll().stream()
				.sorted(Comparator.comparing(InterestCategory::getId))
				.map(category -> new InterestCategoryResponse(category.getId(), category.getName(), category.getIcon()))
				.toList();
	}

	@Transactional
	public SessionResponse setLanguage(UUID sessionId, String language) {
		if (!languageRepository.existsById(language)) {
			throw new BadRequestException("Неподдерживаемый язык");
		}
		User user = getUser(sessionId);
		user.setLanguage(language);
		return toSessionResponse(user);
	}

	@Transactional
	public CitizenshipResultResponse setCitizenship(UUID sessionId, String countryCode) {
		CitizenshipOption option = citizenshipOptionRepository.findById(countryCode)
				.orElseThrow(() -> new BadRequestException("Неизвестная страна гражданства"));
		User user = getUser(sessionId);
		String track = option.getGroupName().equals("eaeu") ? "domestic_equivalent" : "rf_quota_or_paid";
		user.setCitizenship(option.getCode());
		user.setTrack(track);
		String message = track.equals("domestic_equivalent")
				? "Для вас доступен трек поступления наравне с гражданами России"
				: "Для вас доступен трек поступления по квоте или на платное обучение";
		return new CitizenshipResultResponse(track, message, List.of());
	}

	@Transactional(readOnly = true)
	public EgeScoresSubmissionResponse getEgeScores(UUID sessionId) {
		User user = getUser(sessionId);
		return toEgeScoresSubmission(sessionId, user.getLanguage());
	}

	@Transactional
	public EgeScoresSubmissionResponse setEgeScores(UUID sessionId, List<EgeScoreInput> inputs) {
		User user = getUser(sessionId);
		Map<String, EgeScoreInput> scoresBySubject = inputs.stream()
				.collect(Collectors.toMap(EgeScoreInput::subjectId, Function.identity(), (first, second) -> {
					throw new BadRequestException("Предметы ЕГЭ не должны повторяться");
				}));
		List<Subject> subjects = subjectRepository.findAllById(scoresBySubject.keySet());
		if (subjects.size() != scoresBySubject.size()) {
			throw new BadRequestException("Указан неизвестный предмет ЕГЭ");
		}
		for (Subject subject : subjects) {
			Integer score = scoresBySubject.get(subject.getId()).score();
			if (score == null || score < subject.getMinThreshold() || score > 100) {
				throw new BadRequestException(subject.getNameRu() + ": допустимый балл от "
						+ subject.getMinThreshold() + " до 100");
			}
		}

		egeScoreRepository.deleteByUserId(sessionId);
		for (EgeScoreInput input : inputs) {
			EgeScore score = new EgeScore();
			score.setUser(user);
			score.setSubjectId(input.subjectId());
			score.setScore(input.score());
			egeScoreRepository.save(score);
		}
		return toEgeScoresSubmission(sessionId, user.getLanguage());
	}

	@Transactional
	public void setInterests(UUID sessionId, List<String> categoryIds) {
		if (new LinkedHashSet<>(categoryIds).size() != categoryIds.size()) {
			throw new BadRequestException("Категории интересов не должны повторяться");
		}
		List<InterestCategory> categories = interestCategoryRepository.findAllById(categoryIds);
		if (categories.size() != categoryIds.size()) {
			throw new BadRequestException("Указана неизвестная категория интересов");
		}
		getUser(sessionId).setInterests(new LinkedHashSet<>(categories));
	}

	private EgeScoresSubmissionResponse toEgeScoresSubmission(UUID sessionId, String language) {
		List<EgeScore> scores = egeScoreRepository.findByUserId(sessionId);
		Map<String, Subject> subjects = subjectRepository.findAllById(scores.stream()
				.map(EgeScore::getSubjectId)
				.toList())
				.stream()
				.collect(Collectors.toMap(Subject::getId, Function.identity()));
		List<EgeScoreResponse> result = scores.stream()
				.map(score -> toEgeScoreResponse(score, subjects.get(score.getSubjectId()), language))
				.toList();
		return new EgeScoresSubmissionResponse(result, !result.isEmpty() && result.stream().allMatch(EgeScoreResponse::passed));
	}

	private User getUser(UUID sessionId) {
		return userRepository.findById(sessionId)
				.orElseThrow(() -> new ResourceNotFoundException("Заявка не найдена"));
	}

	private SessionResponse toSessionResponse(User user) {
		return new SessionResponse(user.getId(), "max", user.getLanguage(), user.getCitizenship(), user.getCreatedAt());
	}

	private EgeScoreResponse toEgeScoreResponse(EgeScore score, Subject subject, String language) {
		if (subject == null) {
			return new EgeScoreResponse(score.getSubjectId(), score.getSubjectId(), score.getScore(), 0, false);
		}
		return new EgeScoreResponse(score.getSubjectId(), localizedName(subject, language), score.getScore(),
				subject.getMinThreshold(), score.getScore() >= subject.getMinThreshold());
	}

	private String localizedName(Subject subject, String language) {
		String name = switch (language == null ? "ru" : language) {
			case "kk" -> subject.getNameKk();
			case "ky" -> subject.getNameKy();
			default -> subject.getNameRu();
		};
		return name == null || name.isBlank() ? subject.getNameRu() : name;
	}

	private String localizedName(CitizenshipOption option, String language) {
		String name = switch (language == null ? "ru" : language) {
			case "kk" -> option.getNameKk();
			case "ky" -> option.getNameKy();
			default -> option.getNameRu();
		};
		return name == null || name.isBlank() ? option.getNameRu() : name;
	}
}
