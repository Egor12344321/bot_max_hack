package com.runiversityadmisson.bot.presentation.controller.planning;

import com.runiversityadmisson.bot.application.dto.planning.ProgramOptionPageResponse;
import com.runiversityadmisson.bot.application.planning.RecommendationService;
import com.runiversityadmisson.bot.presentation.exception.ResourceNotFoundException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/sessions")
public class RecommendationsController {
	private final RecommendationService recommendationService;

	@GetMapping("/{sessionId}/recommendations")
	public ProgramOptionPageResponse getRecommendations(
			@PathVariable UUID sessionId,
			@AuthenticationPrincipal UUID principal,
			@RequestParam(required = false) String directionId,
			@RequestParam(defaultValue = "0") int offset,
			@RequestParam(defaultValue = "20") int limit) {
		ensureOwner(sessionId, principal);
		return recommendationService.getRecommendations(sessionId, directionId, offset, limit);
	}

	private void ensureOwner(UUID sessionId, UUID authenticatedUserId) {
		if (!sessionId.equals(authenticatedUserId)) {
			throw new ResourceNotFoundException("Заявка не найдена");
		}
	}
}
