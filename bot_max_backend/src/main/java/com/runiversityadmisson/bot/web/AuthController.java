package com.runiversityadmisson.bot.web;

import com.runiversityadmisson.bot.web.auth.JwtService;
import com.runiversityadmisson.bot.web.auth.MaxInitDataValidator;
import com.runiversityadmisson.bot.web.session.ApplicantSession;
import com.runiversityadmisson.bot.web.session.ApplicantSessionService;
import com.runiversityadmisson.bot.web.dto.ExchangeAuthRequest;
import com.runiversityadmisson.bot.web.dto.ExchangeAuthResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/auth")
public class AuthController {

	private final MaxInitDataValidator maxInitDataValidator;
	private final ApplicantSessionService applicantSessionService;
	private final JwtService jwtService;
	private final SessionResponseMapper sessionResponseMapper;

	public AuthController(MaxInitDataValidator maxInitDataValidator,
			ApplicantSessionService applicantSessionService, JwtService jwtService,
			SessionResponseMapper sessionResponseMapper) {
		this.maxInitDataValidator = maxInitDataValidator;
		this.applicantSessionService = applicantSessionService;
		this.jwtService = jwtService;
		this.sessionResponseMapper = sessionResponseMapper;
	}

	@PostMapping("/exchange")
	public ExchangeAuthResponse exchange(@RequestBody ExchangeAuthRequest request) {
		Long maxUserId = maxInitDataValidator.validateAndGetUserId(request.launchParams());
		ApplicantSession session = applicantSessionService.getCompletedByMaxUserId(maxUserId);
		return new ExchangeAuthResponse(jwtService.create(session.getId()), jwtService.getTtlSeconds(),
				sessionResponseMapper.toSession(session));
	}
}
