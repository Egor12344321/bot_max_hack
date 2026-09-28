package com.runiversityadmisson.bot.application.onboarding;

import com.runiversityadmisson.bot.application.catalog.CatalogService;
import com.runiversityadmisson.bot.application.dto.response.AchievementResponse;
import com.runiversityadmisson.bot.application.dto.response.PrivilegeApplyResultResponse;
import com.runiversityadmisson.bot.application.dto.response.PrivilegeCategoryResponse;
import com.runiversityadmisson.bot.domain.applicant.model.PrivilegeCategory;
import com.runiversityadmisson.bot.domain.applicant.model.QuotaType;
import com.runiversityadmisson.bot.domain.applicant.model.User;
import com.runiversityadmisson.bot.domain.applicant.ports.UserRepository;
import com.runiversityadmisson.bot.presentation.exception.BadRequestException;
import com.runiversityadmisson.bot.presentation.exception.ResourceNotFoundException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Шаги 2 и 3 онбординга в мини-аппе: индивидуальные достижения (ИД) и льготы.
 */
@Service
@RequiredArgsConstructor
public class AchievementPrivilegeService {

	private final UserRepository userRepository;
	private final CatalogService catalogService;

	public List<AchievementResponse> getAchievements() {
		return catalogService.getAchievements().stream()
				.map(achievement -> new AchievementResponse(achievement.id(), achievement.name(), achievement.description()))
				.toList();
	}

	public List<PrivilegeCategoryResponse> getPrivilegeCategories() {
		return catalogService.getPrivilegeCategories().stream()
				.map(category -> new PrivilegeCategoryResponse(
						category.id(),
						category.name(),
						category.quotaType().getCode(),
						category.maxQuotaPercent(),
						category.requiredDocuments() == null ? List.of() : category.requiredDocuments()))
				.toList();
	}

	@Transactional
	public void setAchievements(UUID sessionId, List<String> achievementIds) {
		ensureNoDuplicates(achievementIds, "Достижения не должны повторяться");
		if (!achievementIds.stream().allMatch(catalogService::achievementExists)) {
			throw new BadRequestException("Указано неизвестное достижение");
		}
		User user = getUser(sessionId);
		user.getAchievementIds().clear();
		user.getAchievementIds().addAll(achievementIds);
	}

	@Transactional
	public PrivilegeApplyResultResponse setPrivileges(UUID sessionId, List<String> categoryIds) {
		ensureNoDuplicates(categoryIds, "Льготы не должны повторяться");
		List<PrivilegeCategory> categories = categoryIds.stream()
				.map(id -> catalogService.findPrivilegeCategory(id)
						.orElseThrow(() -> new BadRequestException("Указана неизвестная льгота")))
				.toList();
		User user = getUser(sessionId);
		user.getPrivilegeCategoryIds().clear();
		user.getPrivilegeCategoryIds().addAll(categoryIds);

		QuotaType bestQuotaType = QuotaType.best(categories.stream().map(PrivilegeCategory::quotaType).toList());
		return new PrivilegeApplyResultResponse(List.copyOf(categoryIds), bestQuotaType.getCode(), message(bestQuotaType));
	}

	private static void ensureNoDuplicates(List<String> ids, String message) {
		if (new LinkedHashSet<>(ids).size() != ids.size()) {
			throw new BadRequestException(message);
		}
	}

	private User getUser(UUID sessionId) {
		return userRepository.findById(sessionId)
				.orElseThrow(() -> new ResourceNotFoundException("Заявка не найдена"));
	}

	private static String message(QuotaType quotaType) {
		return switch (quotaType) {
			case BVI -> "Применено зачисление без вступительных испытаний (БВИ)";
			case SPECIAL_QUOTA -> "Применена особая квота";
			case SEPARATE_QUOTA -> "Применена отдельная квота";
			case TARGET_QUOTA -> "Применена целевая квота";
			case NONE -> "Льготы не применяются";
		};
	}
}
