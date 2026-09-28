package com.runiversityadmisson.bot.presentation.controller;

import com.runiversityadmisson.bot.application.dto.request.SetCitizenshipRequest;
import com.runiversityadmisson.bot.application.dto.request.SetEgeScoresRequest;
import com.runiversityadmisson.bot.application.dto.request.SetInterestsRequest;
import com.runiversityadmisson.bot.application.dto.request.SetLanguageRequest;
import com.runiversityadmisson.bot.application.dto.response.CitizenshipResultResponse;
import com.runiversityadmisson.bot.application.dto.response.EgeScoresSubmissionResponse;
import com.runiversityadmisson.bot.application.dto.response.SessionResponse;
import com.runiversityadmisson.bot.application.onboarding.OnboardingApiService;
import com.runiversityadmisson.bot.presentation.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
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

	private void ensureOwner(UUID sessionId, UUID authenticatedUserId) {
		if (!sessionId.equals(authenticatedUserId)) {
			throw new ResourceNotFoundException("Заявка не найдена");
		}
	}
}
