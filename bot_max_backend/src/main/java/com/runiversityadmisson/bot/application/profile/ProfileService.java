package com.runiversityadmisson.bot.application.profile;

import com.runiversityadmisson.bot.application.benefit.PrivilegeCatalogService;
import com.runiversityadmisson.bot.application.dto.direction.InterestCategoryResponse;
import com.runiversityadmisson.bot.application.dto.direction.StudyDirectionResponse;
import com.runiversityadmisson.bot.application.dto.exam.EgeScoreResponse;
import com.runiversityadmisson.bot.application.dto.planning.ProgramOptionResponse;
import com.runiversityadmisson.bot.application.dto.profile.ProfileProgramsResponse;
import com.runiversityadmisson.bot.application.dto.profile.ProfileResponse;
import com.runiversityadmisson.bot.application.olympiad.OlympiadService;
import com.runiversityadmisson.bot.application.onboarding.QuestionnaireService;
import com.runiversityadmisson.bot.application.planning.RecommendationService;
import com.runiversityadmisson.bot.domain.applicant.model.benefit.Achievement;
import com.runiversityadmisson.bot.domain.applicant.model.benefit.PrivilegeCategory;
import com.runiversityadmisson.bot.domain.applicant.model.direction.StudyDirection;
import com.runiversityadmisson.bot.domain.applicant.model.profile.User;
import com.runiversityadmisson.bot.domain.applicant.ports.benefit.AchievementRepository;
import com.runiversityadmisson.bot.domain.applicant.ports.direction.StudyDirectionRepository;
import com.runiversityadmisson.bot.domain.applicant.ports.profile.UserRepository;
import com.runiversityadmisson.bot.presentation.exception.ResourceNotFoundException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Экран «Профиль»: данные пользователя и сводка по рекомендациям. */
@Service
@RequiredArgsConstructor
public class ProfileService {

	private final UserRepository userRepository;
	private final StudyDirectionRepository directionRepository;
	private final AchievementRepository achievementRepository;
	private final PrivilegeCatalogService privilegeCatalogService;
	private final QuestionnaireService questionnaireService;
	private final OlympiadService olympiadService;
	private final RecommendationService recommendationService;

	@Transactional(readOnly = true)
	public ProfileResponse getProfile(UUID sessionId) {
		User user = userRepository.findById(sessionId)
				.orElseThrow(() -> new ResourceNotFoundException("Заявка не найдена"));

		List<EgeScoreResponse> egeScores = questionnaireService.getEgeScores(sessionId).scores();
		Map<String, StudyDirection> directionsById = directionRepository.findAllById(user.getDirectionIds()).stream()
				.collect(Collectors.toMap(StudyDirection::getId, Function.identity()));
		List<StudyDirectionResponse> directions = user.getDirectionIds().stream()
				.map(directionsById::get)
				.filter(Objects::nonNull)
				.map(direction -> new StudyDirectionResponse(direction.getId(), direction.getCode(), direction.getName()))
				.toList();
		ProfileProgramsResponse programs = summarize(recommendationService.getAllRecommendations(sessionId));

		return new ProfileResponse(
				user.getLanguage(),
				user.getCitizenship(),
				egeScores,
				egeScores.stream().mapToInt(EgeScoreResponse::score).sum(),
				user.getInterests().stream()
						.map(category -> new InterestCategoryResponse(category.getId(), category.getName(), category.getIcon()))
						.toList(),
				directions,
				olympiadService.getDiplomas(sessionId),
				achievementRepository.findAllById(user.getAchievementIds()).stream()
						.sorted(Comparator.comparing(Achievement::getSortOrder))
						.map(Achievement::getName)
						.toList(),
				user.getPrivilegeCategoryIds().stream()
						.map(privilegeCatalogService::findPrivilegeCategory)
						.flatMap(Optional::stream)
						.map(PrivilegeCategory::name)
						.toList(),
				programs,
				advice(egeScores.isEmpty(), directions.isEmpty(), programs));
	}

	static ProfileProgramsResponse summarize(List<ProgramOptionResponse> options) {
		Map<String, Long> byComparison = options.stream()
				.collect(Collectors.groupingBy(ProgramOptionResponse::comparison, Collectors.counting()));
		return new ProfileProgramsResponse(
				options.size(),
				count(byComparison, "bvi"),
				count(byComparison, "above_previous"),
				count(byComparison, "near_previous"),
				count(byComparison, "below_previous"),
				count(byComparison, "insufficient_data"),
				(int) options.stream().filter(option -> "reserve".equals(option.riskStatus())).count(),
				(int) options.stream().filter(option -> "real".equals(option.riskStatus())).count(),
				(int) options.stream().filter(option -> "risk".equals(option.riskStatus())).count());
	}

	static String advice(boolean noEgeScores, boolean noDirections, ProfileProgramsResponse programs) {
		if (noEgeScores) {
			return "Добавь баллы ЕГЭ: без них подборку не посчитать.";
		}
		if (noDirections) {
			return "Выбери направления в разделе интересов, чтобы получить подборку вузов.";
		}
		if (programs.total() == 0) {
			return "По выбранным направлениям подходящих программ нет. Проверь баллы ЕГЭ или добавь направления.";
		}
		if (programs.bvi() > 0) {
			return "Программ с доступным БВИ: " + programs.bvi() + ". Использовать БВИ можно только в одном вузе "
					+ "и на одном направлении, поэтому выбери самый сильный вариант.";
		}
		if (programs.riskCount() > programs.reserveCount() + programs.realCount()) {
			return "Большинство программ ниже прошлогоднего проходного. Добавь в план программы с запасом, "
					+ "чтобы подстраховаться.";
		}
		if (programs.reserveCount() > 0) {
			return "Программ с запасом: " + programs.reserveCount() + ". Оставь их в плане как страховку, "
					+ "а более сильные вузы ставь выше по приоритету.";
		}
		return "Подборка готова. Открой её, чтобы собрать план поступления.";
	}

	private static int count(Map<String, Long> byComparison, String comparison) {
		return byComparison.getOrDefault(comparison, 0L).intValue();
	}
}
