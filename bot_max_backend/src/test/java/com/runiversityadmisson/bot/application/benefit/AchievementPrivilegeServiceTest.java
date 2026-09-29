package com.runiversityadmisson.bot.application.benefit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.runiversityadmisson.bot.application.catalog.CatalogService;
import com.runiversityadmisson.bot.application.dto.benefit.AchievementResponse;
import com.runiversityadmisson.bot.application.dto.benefit.PrivilegeApplyResultResponse;
import com.runiversityadmisson.bot.application.dto.benefit.PrivilegeCategoryResponse;
import com.runiversityadmisson.bot.domain.applicant.model.benefit.Achievement;
import com.runiversityadmisson.bot.domain.applicant.model.profile.User;
import com.runiversityadmisson.bot.domain.applicant.ports.benefit.AchievementRepository;
import com.runiversityadmisson.bot.domain.applicant.ports.profile.UserRepository;
import com.runiversityadmisson.bot.presentation.exception.BadRequestException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class AchievementPrivilegeServiceTest {

	private final CatalogService catalogService = new CatalogService(JsonMapper.builder().build());
	private final List<Achievement> catalog = List.of(
			achievement("medal_gold", "medal"),
			achievement("medal_silver", "medal"),
			achievement("gto_gold", null),
			achievement("volunteering", null),
			achievement("essay", null));

	private User user;
	private AdmissionBenefitService admissionBenefitService;
	private AchievementPrivilegeService service;

	@BeforeEach
	void setUp() {
		user = new User();
		user.setId(UUID.randomUUID());
		UserRepository userRepository = mock(UserRepository.class);
		when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
		AchievementRepository achievementRepository = mock(AchievementRepository.class);
		when(achievementRepository.findAllByOrderBySortOrderAsc()).thenReturn(catalog);
		when(achievementRepository.findAllById(anyIterable())).thenAnswer(invocation -> {
			Iterable<String> ids = invocation.getArgument(0);
			List<String> requested = StreamSupport.stream(ids.spliterator(), false).toList();
			return catalog.stream().filter(achievement -> requested.contains(achievement.getId())).toList();
		});
		admissionBenefitService = mock(AdmissionBenefitService.class);
		service = new AchievementPrivilegeService(userRepository, achievementRepository, catalogService,
				admissionBenefitService);
	}

	@Test
	void returnsAchievementsWithExclusiveGroups() {
		List<AchievementResponse> achievements = service.getAchievements();

		assertThat(achievements).extracting(AchievementResponse::id)
				.containsExactly("medal_gold", "medal_silver", "gto_gold", "volunteering", "essay");
		assertThat(achievements.getFirst().exclusiveGroup()).isEqualTo("medal");
	}

	@Test
	void privilegeCatalogHasNoBvi() {
		List<PrivilegeCategoryResponse> categories = service.getPrivilegeCategories();

		assertThat(categories).extracting(PrivilegeCategoryResponse::quotaType)
				.contains("special_quota", "separate_quota", "target_quota")
				.doesNotContain("bvi");
	}

	@Test
	void replacesSelectedAchievements() {
		service.setAchievements(user.getId(), List.of("gto_gold", "volunteering"));
		service.setAchievements(user.getId(), List.of("essay"));

		assertThat(user.getAchievementIds()).containsExactly("essay");
	}

	@Test
	void rejectsUnknownAchievement() {
		assertThatThrownBy(() -> service.setAchievements(user.getId(), List.of("olympiad")))
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
	void rejectsTwoMedals() {
		assertThatThrownBy(() -> service.setAchievements(user.getId(), List.of("medal_gold", "medal_silver")))
				.isInstanceOf(BadRequestException.class)
				.hasMessage("Из этих достижений можно выбрать только одно");
	}

	@Test
	void olympiadBviBeatsQuotas() {
		when(admissionBenefitService.countBviPrograms(user.getId())).thenReturn(2L);

		PrivilegeApplyResultResponse result = service.setPrivileges(user.getId(), List.of("target_contract", "svo"));

		assertThat(result.bestQuotaType()).isEqualTo("bvi");
		assertThat(result.message()).contains("направлений: 2");
		assertThat(result.selectedCategoryIds()).containsExactly("target_contract", "svo");
		assertThat(user.getPrivilegeCategoryIds()).containsExactlyInAnyOrder("target_contract", "svo");
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
		assertThatThrownBy(() -> service.setPrivileges(user.getId(), List.of("olympiad_bvi")))
				.isInstanceOf(BadRequestException.class)
				.hasMessage("Указана неизвестная льгота");
	}

	private static Achievement achievement(String id, String exclusiveGroup) {
		Achievement achievement = new Achievement();
		achievement.setId(id);
		achievement.setName(id);
		achievement.setExclusiveGroup(exclusiveGroup);
		return achievement;
	}
}
