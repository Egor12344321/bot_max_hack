package com.runiversityadmisson.bot.presentation.controller;

import com.runiversityadmisson.bot.web.dto.SessionDraftResponse;
import com.runiversityadmisson.bot.web.exception.ResourceNotFoundException;
import com.runiversityadmisson.bot.web.service.SessionQueryService;
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

	private final SessionQueryService sessionQueryService;

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
