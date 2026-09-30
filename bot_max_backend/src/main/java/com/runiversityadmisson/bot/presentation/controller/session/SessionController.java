package com.runiversityadmisson.bot.presentation.controller.session;

import com.runiversityadmisson.bot.application.dto.session.SessionDraftResponse;
import com.runiversityadmisson.bot.application.session.SessionProfileService;
import com.runiversityadmisson.bot.presentation.exception.ResourceNotFoundException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/sessions")
public class SessionController {

	private final SessionProfileService sessionQueryService;

	@org.springframework.web.bind.annotation.PostMapping("/{sessionId}/onboarding/complete")
	public SessionDraftResponse completeOnboarding(@PathVariable UUID sessionId,
			@AuthenticationPrincipal UUID principal) {
		if (!sessionId.equals(principal)) throw new ResourceNotFoundException("Заявка не найдена");
		return sessionQueryService.completeOnboarding(sessionId);
	}

	@GetMapping("/{sessionId}")
	public SessionDraftResponse getSession(
			@PathVariable UUID sessionId,
			@AuthenticationPrincipal UUID authenticatedUserId) {
		if (!sessionId.equals(authenticatedUserId)) {
			throw new ResourceNotFoundException("Заявка не найдена");
		}
		return sessionQueryService.getSession(sessionId);
	}
}
