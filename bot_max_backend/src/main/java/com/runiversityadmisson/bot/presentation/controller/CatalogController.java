package com.runiversityadmisson.bot.presentation.controller;

import com.runiversityadmisson.bot.application.dto.response.AchievementResponse;
import com.runiversityadmisson.bot.application.dto.response.CitizenshipOptionResponse;
import com.runiversityadmisson.bot.application.dto.response.InterestCategoryResponse;
import com.runiversityadmisson.bot.application.dto.response.LanguageResponse;
import com.runiversityadmisson.bot.application.dto.response.PrivilegeCategoryResponse;
import com.runiversityadmisson.bot.application.dto.response.SubjectResponse;
import com.runiversityadmisson.bot.application.onboarding.AchievementPrivilegeService;
import com.runiversityadmisson.bot.application.onboarding.OnboardingApiService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1")
public class CatalogController {

	private final OnboardingApiService onboardingApiService;
	private final AchievementPrivilegeService achievementPrivilegeService;

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
}
