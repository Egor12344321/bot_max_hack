package com.runiversityadmisson.bot.presentation.controller.direction;

import com.runiversityadmisson.bot.application.dto.direction.StudyDirectionSelection;
import com.runiversityadmisson.bot.application.dto.direction.StudyDirectionPageResponse;
import com.runiversityadmisson.bot.application.direction.StudyDirectionService;
import com.runiversityadmisson.bot.presentation.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1")
public class StudyDirectionsController {
	private final StudyDirectionService directionService;

	@GetMapping("/directions")
	public StudyDirectionPageResponse search(
			@RequestParam(required = false) String interestCategoryId,
			@RequestParam(required = false) String query,
			@RequestParam(defaultValue = "0") int offset,
			@RequestParam(defaultValue = "20") int limit) {
		return directionService.search(interestCategoryId, query, offset, limit);
	}

	@GetMapping("/sessions/{sessionId}/directions")
	public StudyDirectionSelection getSelection(@PathVariable UUID sessionId,
			@AuthenticationPrincipal UUID principal) {
		ensureOwner(sessionId, principal);
		return directionService.getSelection(sessionId);
	}

	@PutMapping("/sessions/{sessionId}/directions")
	public StudyDirectionSelection replaceSelection(@PathVariable UUID sessionId,
			@AuthenticationPrincipal UUID principal, @Valid @RequestBody StudyDirectionSelection request) {
		ensureOwner(sessionId, principal);
		return directionService.replaceSelection(sessionId, request.directionIds());
	}

	private void ensureOwner(UUID sessionId, UUID authenticatedUserId) {
		if (!sessionId.equals(authenticatedUserId)) {
			throw new ResourceNotFoundException("Заявка не найдена");
		}
	}
}
