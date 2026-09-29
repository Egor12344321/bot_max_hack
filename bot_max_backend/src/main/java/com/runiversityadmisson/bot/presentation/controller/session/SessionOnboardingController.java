package com.runiversityadmisson.bot.presentation.controller.session;

import com.runiversityadmisson.bot.application.dto.benefit.SetAchievementsRequest;
import com.runiversityadmisson.bot.application.dto.profile.SetCitizenshipRequest;
import com.runiversityadmisson.bot.application.dto.exam.SetEgeScoresRequest;
import com.runiversityadmisson.bot.application.dto.direction.SetInterestsRequest;
import com.runiversityadmisson.bot.application.dto.profile.SetLanguageRequest;
import com.runiversityadmisson.bot.application.dto.olympiad.SetOlympiadDiplomasRequest;
import com.runiversityadmisson.bot.application.dto.benefit.SetPrivilegesRequest;
import com.runiversityadmisson.bot.application.dto.profile.CitizenshipResultResponse;
import com.runiversityadmisson.bot.application.dto.exam.EgeScoresSubmissionResponse;
import com.runiversityadmisson.bot.application.dto.olympiad.OlympiadDiplomaResponse;
import com.runiversityadmisson.bot.application.dto.benefit.PrivilegeApplyResultResponse;
import com.runiversityadmisson.bot.application.dto.session.SessionResponse;
import com.runiversityadmisson.bot.application.benefit.AchievementPrivilegeService;
import com.runiversityadmisson.bot.application.olympiad.OlympiadService;
import com.runiversityadmisson.bot.application.onboarding.OnboardingApiService;
import com.runiversityadmisson.bot.presentation.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/sessions")
public class SessionOnboardingController {

	private final OnboardingApiService onboardingApiService;
	private final AchievementPrivilegeService achievementPrivilegeService;
	private final OlympiadService olympiadService;

	@PatchMapping("/{sessionId}/language")
	public SessionResponse setLanguage(
			@PathVariable UUID sessionId,
			@AuthenticationPrincipal UUID authenticatedUserId,
			@Valid @RequestBody SetLanguageRequest request) {
		ensureOwner(sessionId, authenticatedUserId);
		return onboardingApiService.setLanguage(sessionId, request.language());
	}

	@PatchMapping("/{sessionId}/citizenship")
	public CitizenshipResultResponse setCitizenship(
			@PathVariable UUID sessionId,
			@AuthenticationPrincipal UUID authenticatedUserId,
			@Valid @RequestBody SetCitizenshipRequest request) {
		ensureOwner(sessionId, authenticatedUserId);
		return onboardingApiService.setCitizenship(sessionId, request.countryCode());
	}

	@GetMapping("/{sessionId}/ege-scores")
	public EgeScoresSubmissionResponse getEgeScores(
			@PathVariable UUID sessionId,
			@AuthenticationPrincipal UUID authenticatedUserId) {
		ensureOwner(sessionId, authenticatedUserId);
		return onboardingApiService.getEgeScores(sessionId);
	}

	@PutMapping("/{sessionId}/ege-scores")
	public EgeScoresSubmissionResponse setEgeScores(
			@PathVariable UUID sessionId,
			@AuthenticationPrincipal UUID authenticatedUserId,
			@Valid @RequestBody SetEgeScoresRequest request) {
		ensureOwner(sessionId, authenticatedUserId);
		return onboardingApiService.setEgeScores(sessionId, request.scores());
	}

	@PutMapping("/{sessionId}/interests")
	public ResponseEntity<Void> setInterests(
			@PathVariable UUID sessionId,
			@AuthenticationPrincipal UUID authenticatedUserId,
			@Valid @RequestBody SetInterestsRequest request) {
		ensureOwner(sessionId, authenticatedUserId);
		onboardingApiService.setInterests(sessionId, request.categoryIds());
		return ResponseEntity.ok().build();
	}

	@PutMapping("/{sessionId}/achievements")
	public ResponseEntity<Void> setAchievements(
			@PathVariable UUID sessionId,
			@AuthenticationPrincipal UUID authenticatedUserId,
			@Valid @RequestBody SetAchievementsRequest request) {
		ensureOwner(sessionId, authenticatedUserId);
		achievementPrivilegeService.setAchievements(sessionId, request.achievementIds());
		return ResponseEntity.ok().build();
	}

	@GetMapping("/{sessionId}/olympiads")
	public List<OlympiadDiplomaResponse> getOlympiadDiplomas(
			@PathVariable UUID sessionId,
			@AuthenticationPrincipal UUID authenticatedUserId) {
		ensureOwner(sessionId, authenticatedUserId);
		return olympiadService.getDiplomas(sessionId);
	}

	@PutMapping("/{sessionId}/olympiads")
	public List<OlympiadDiplomaResponse> setOlympiadDiplomas(
			@PathVariable UUID sessionId,
			@AuthenticationPrincipal UUID authenticatedUserId,
			@Valid @RequestBody SetOlympiadDiplomasRequest request) {
		ensureOwner(sessionId, authenticatedUserId);
		return olympiadService.setDiplomas(sessionId, request.diplomas());
	}

	@PutMapping("/{sessionId}/privilege")
	public PrivilegeApplyResultResponse setPrivileges(
			@PathVariable UUID sessionId,
			@AuthenticationPrincipal UUID authenticatedUserId,
			@Valid @RequestBody SetPrivilegesRequest request) {
		ensureOwner(sessionId, authenticatedUserId);
		return achievementPrivilegeService.setPrivileges(sessionId, request.categoryIds());
	}

	private void ensureOwner(UUID sessionId, UUID authenticatedUserId) {
		if (!sessionId.equals(authenticatedUserId)) {
			throw new ResourceNotFoundException("Заявка не найдена");
		}
	}
}
