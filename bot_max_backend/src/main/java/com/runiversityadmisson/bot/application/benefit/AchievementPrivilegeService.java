package com.runiversityadmisson.bot.application.benefit;

import com.runiversityadmisson.bot.application.catalog.CatalogService;
import com.runiversityadmisson.bot.application.dto.benefit.AchievementResponse;
import com.runiversityadmisson.bot.application.dto.benefit.PrivilegeApplyResultResponse;
import com.runiversityadmisson.bot.application.dto.benefit.PrivilegeCategoryResponse;
import com.runiversityadmisson.bot.domain.applicant.model.benefit.Achievement;
import com.runiversityadmisson.bot.domain.applicant.model.benefit.PrivilegeCategory;
import com.runiversityadmisson.bot.domain.applicant.model.benefit.QuotaType;
import com.runiversityadmisson.bot.domain.applicant.model.profile.User;
import com.runiversityadmisson.bot.domain.applicant.ports.benefit.AchievementRepository;
import com.runiversityadmisson.bot.domain.applicant.ports.profile.UserRepository;
import com.runiversityadmisson.bot.presentation.exception.BadRequestException;
import com.runiversityadmisson.bot.presentation.exception.ResourceNotFoundException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
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
	private final AchievementRepository achievementRepository;
	private final CatalogService catalogService;
	private final AdmissionBenefitService admissionBenefitService;

	@Transactional(readOnly = true)
	public List<AchievementResponse> getAchievements() {
		return achievementRepository.findAllByOrderBySortOrderAsc().stream()
				.map(achievement -> new AchievementResponse(achievement.getId(), achievement.getName(),
						achievement.getDescription(), achievement.getExclusiveGroup()))
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
		List<Achievement> achievements = achievementRepository.findAllById(achievementIds);
		if (achievements.size() != achievementIds.size()) {
			throw new BadRequestException("Указано неизвестное достижение");
		}
		List<String> groups = achievements.stream()
				.map(Achievement::getExclusiveGroup)
				.filter(Objects::nonNull)
				.toList();
		if (new LinkedHashSet<>(groups).size() != groups.size()) {
			throw new BadRequestException("Из этих достижений можно выбрать только одно");
		}
		User user = getUser(sessionId);
		user.getAchievementIds().clear();
		user.getAchievementIds().addAll(achievementIds);
	}

	/**
	 * БВИ среди категорий нет: его дают дипломы олимпиад, и только в конкретных вузах.
	 * Если дипломы пользователя дают БВИ хотя бы на одном направлении, лучшей льготой считается БВИ.
	 */
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

		List<QuotaType> quotaTypes = new ArrayList<>(categories.stream().map(PrivilegeCategory::quotaType).toList());
		long bviPrograms = admissionBenefitService.countBviPrograms(sessionId);
		if (bviPrograms > 0) {
			quotaTypes.add(QuotaType.BVI);
		}
		QuotaType bestQuotaType = QuotaType.best(quotaTypes);
		return new PrivilegeApplyResultResponse(List.copyOf(categoryIds), bestQuotaType.getCode(),
				message(bestQuotaType, bviPrograms));
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

	private static String message(QuotaType quotaType, long bviPrograms) {
		return switch (quotaType) {
			case BVI -> "Дипломы олимпиад дают БВИ (зачисление без вступительных испытаний), направлений: "
					+ bviPrograms + ". БВИ можно использовать только один раз";
			case SPECIAL_QUOTA -> "Применена особая квота";
			case SEPARATE_QUOTA -> "Применена отдельная квота";
			case TARGET_QUOTA -> "Применена целевая квота";
			case NONE -> "Льготы не применяются";
		};
	}
}
