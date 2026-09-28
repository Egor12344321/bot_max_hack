package com.runiversityadmisson.bot.application.onboarding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.runiversityadmisson.bot.application.catalog.CatalogService;
import com.runiversityadmisson.bot.application.dto.response.AchievementResponse;
import com.runiversityadmisson.bot.application.dto.response.PrivilegeApplyResultResponse;
import com.runiversityadmisson.bot.application.dto.response.PrivilegeCategoryResponse;
import com.runiversityadmisson.bot.domain.applicant.model.User;
import com.runiversityadmisson.bot.domain.applicant.ports.UserRepository;
import com.runiversityadmisson.bot.presentation.exception.BadRequestException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class AchievementPrivilegeServiceTest {

	private final CatalogService catalogService = new CatalogService(JsonMapper.builder().build());

	private User user;
	private AchievementPrivilegeService service;

	@BeforeEach
	void setUp() {
		user = new User();
		user.setId(UUID.randomUUID());
		UserRepository userRepository = mock(UserRepository.class);
		when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
		service = new AchievementPrivilegeService(userRepository, catalogService);
	}

	@Test
	void loadsAchievementsFromJson() {
		List<AchievementResponse> achievements = service.getAchievements();

		assertThat(achievements).isNotEmpty();
		assertThat(achievements.getFirst().id()).isEqualTo("gto_gold");
	}

	@Test
	void loadsPrivilegeCategoriesWithQuotaCodes() {
		List<PrivilegeCategoryResponse> categories = service.getPrivilegeCategories();

		assertThat(categories).extracting(PrivilegeCategoryResponse::quotaType)
				.contains("bvi", "special_quota", "separate_quota", "target_quota");
	}

	@Test
	void replacesSelectedAchievements() {
		service.setAchievements(user.getId(), List.of("gto_gold", "volunteering"));
		service.setAchievements(user.getId(), List.of("essay"));

		assertThat(user.getAchievementIds()).containsExactly("essay");
	}

	@Test
	void rejectsUnknownAchievement() {
		assertThatThrownBy(() -> service.setAchievements(user.getId(), List.of("unknown")))
				.isInstanceOf(BadRequestException.class)
				.hasMessage("Указано неизвестное достижение");
	}

	@Test
	void rejectsDuplicateAchievements() {
		assertThatThrownBy(() -> service.setAchievements(user.getId(), List.of("essay", "essay")))
				.isInstanceOf(BadRequestException.class)
				.hasMessage("Достижения не должны повторяться");
	}

	@Test
	void picksMostProfitableQuota() {
		PrivilegeApplyResultResponse result = service.setPrivileges(user.getId(),
				List.of("target_contract", "svo", "olympiad_bvi"));

		assertThat(result.bestQuotaType()).isEqualTo("bvi");
		assertThat(result.selectedCategoryIds()).containsExactly("target_contract", "svo", "olympiad_bvi");
		assertThat(user.getPrivilegeCategoryIds()).containsExactlyInAnyOrder("target_contract", "svo", "olympiad_bvi");
	}

	@Test
	void specialQuotaBeatsSeparateAndTarget() {
		PrivilegeApplyResultResponse result = service.setPrivileges(user.getId(),
				List.of("target_contract", "svo", "orphan"));

		assertThat(result.bestQuotaType()).isEqualTo("special_quota");
	}

	@Test
	void emptyPrivilegesMeanNoQuota() {
		user.getPrivilegeCategoryIds().add("svo");

		PrivilegeApplyResultResponse result = service.setPrivileges(user.getId(), List.of());

		assertThat(result.bestQuotaType()).isEqualTo("none");
		assertThat(user.getPrivilegeCategoryIds()).isEmpty();
	}

	@Test
	void rejectsUnknownPrivilege() {
		assertThatThrownBy(() -> service.setPrivileges(user.getId(), List.of("unknown")))
				.isInstanceOf(BadRequestException.class)
				.hasMessage("Указана неизвестная льгота");
	}
}
