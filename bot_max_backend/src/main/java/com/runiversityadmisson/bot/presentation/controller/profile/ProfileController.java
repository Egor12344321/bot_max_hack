package com.runiversityadmisson.bot.presentation.controller.profile;

import com.runiversityadmisson.bot.application.dto.profile.ProfileResponse;
import com.runiversityadmisson.bot.application.profile.ProfileService;
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
public class ProfileController {
	private final ProfileService profileService;

	@GetMapping("/{sessionId}/profile")
	public ProfileResponse getProfile(@PathVariable UUID sessionId, @AuthenticationPrincipal UUID principal) {
		ensureOwner(sessionId, principal);
		return profileService.getProfile(sessionId);
	}

	private void ensureOwner(UUID sessionId, UUID authenticatedUserId) {
		if (!sessionId.equals(authenticatedUserId)) {
			throw new ResourceNotFoundException("Заявка не найдена");
		}
	}
}
