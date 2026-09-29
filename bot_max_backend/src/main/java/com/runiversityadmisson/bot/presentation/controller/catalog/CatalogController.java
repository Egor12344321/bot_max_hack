package com.runiversityadmisson.bot.presentation.controller.catalog;

import com.runiversityadmisson.bot.application.dto.benefit.AchievementResponse;
import com.runiversityadmisson.bot.application.dto.profile.CitizenshipOptionResponse;
import com.runiversityadmisson.bot.application.dto.direction.InterestCategoryResponse;
import com.runiversityadmisson.bot.application.dto.profile.LanguageResponse;
import com.runiversityadmisson.bot.application.dto.olympiad.OlympiadResponse;
import com.runiversityadmisson.bot.application.dto.benefit.PrivilegeCategoryResponse;
import com.runiversityadmisson.bot.application.dto.exam.SubjectResponse;
import com.runiversityadmisson.bot.application.benefit.AchievementPrivilegeService;
import com.runiversityadmisson.bot.application.olympiad.OlympiadService;
import com.runiversityadmisson.bot.application.onboarding.QuestionnaireService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1")
public class CatalogController {

	private final QuestionnaireService onboardingApiService;
	private final AchievementPrivilegeService achievementPrivilegeService;
	private final OlympiadService olympiadService;

	@GetMapping("/languages")
	public List<LanguageResponse> getLanguages() {
		return onboardingApiService.getLanguages();
	}

	@GetMapping("/citizenship-groups")
	public List<CitizenshipOptionResponse> getCitizenshipOptions(
			@AuthenticationPrincipal UUID authenticatedUserId) {
		return onboardingApiService.getCitizenshipOptions(authenticatedUserId);
	}

	@GetMapping("/subjects")
	public List<SubjectResponse> getSubjects(@AuthenticationPrincipal UUID authenticatedUserId) {
		return onboardingApiService.getSubjects(authenticatedUserId);
	}

	@GetMapping("/interest-categories")
	public List<InterestCategoryResponse> getInterestCategories() {
		return onboardingApiService.getInterestCategories();
	}

	@GetMapping("/achievements")
	public List<AchievementResponse> getAchievements() {
		return achievementPrivilegeService.getAchievements();
	}

	@GetMapping("/privilege-categories")
	public List<PrivilegeCategoryResponse> getPrivilegeCategories() {
		return achievementPrivilegeService.getPrivilegeCategories();
	}

	@GetMapping("/olympiads")
	public List<OlympiadResponse> getOlympiads(
			@RequestParam(required = false) String query,
			@RequestParam(name = "subject", required = false) String subjectId) {
		return olympiadService.getOlympiads(query, subjectId);
	}
}
