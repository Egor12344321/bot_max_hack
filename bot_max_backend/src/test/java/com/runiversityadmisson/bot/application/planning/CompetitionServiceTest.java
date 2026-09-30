package com.runiversityadmisson.bot.application.planning;

import static org.assertj.core.api.Assertions.assertThat;

import com.runiversityadmisson.bot.application.benefit.PrivilegeCatalogService;
import com.runiversityadmisson.bot.domain.applicant.model.profile.User;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class CompetitionServiceTest {
	private final CompetitionService service = new CompetitionService(new PrivilegeCatalogService(new ObjectMapper()));

	@Test
	void recognizesLegacyAndNewSvoCategoriesAsSeparateQuota() {
		for (String id : java.util.List.of("svo", "svo_participant", "svo_child")) {
			User user = new User();
			user.getPrivilegeCategoryIds().add(id);
			assertThat(service.available(user)).containsExactly("separate_quota", "general");
		}
	}

	@Test
	void preservesGeneralCompetitionChoiceAndRevokesRemovedQuota() {
		User user = new User();
		user.getPrivilegeCategoryIds().add("svo_participant");
		user.setPreferredCompetitionType("general");
		assertThat(service.available(user)).containsExactly("general", "separate_quota");
		user.setPreferredCompetitionType("separate_quota");
		user.getPrivilegeCategoryIds().clear();
		assertThat(service.available(user)).containsExactly("general");
	}

	@Test
	void multipleGroundsDoNotDuplicateCompetitionTypes() {
		User user = new User();
		user.getPrivilegeCategoryIds().addAll(java.util.List.of("svo", "svo_child", "orphan", "child_disability"));
		assertThat(service.available(user)).containsExactly("special_quota", "separate_quota", "general");
	}
}
