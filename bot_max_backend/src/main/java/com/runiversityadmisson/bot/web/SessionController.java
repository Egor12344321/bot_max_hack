package com.runiversityadmisson.bot.web;

import com.runiversityadmisson.bot.web.session.ApplicantSessionService;
import com.runiversityadmisson.bot.web.dto.SessionDraftResponse;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/sessions")
public class SessionController {

	private final ApplicantSessionService applicantSessionService;
	private final SessionResponseMapper sessionResponseMapper;

	public SessionController(ApplicantSessionService applicantSessionService,
			SessionResponseMapper sessionResponseMapper) {
		this.applicantSessionService = applicantSessionService;
		this.sessionResponseMapper = sessionResponseMapper;
	}

	@GetMapping("/{sessionId}")
	public SessionDraftResponse getSession(@PathVariable UUID sessionId) {
		return sessionResponseMapper.toDraft(applicantSessionService.getById(sessionId));
	}
}
