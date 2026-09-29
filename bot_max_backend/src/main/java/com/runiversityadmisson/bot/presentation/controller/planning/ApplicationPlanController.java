package com.runiversityadmisson.bot.presentation.controller.planning;

import com.runiversityadmisson.bot.application.dto.planning.ApplicationPlanResponse;
import com.runiversityadmisson.bot.application.dto.planning.PlanPreviewResponse;
import com.runiversityadmisson.bot.application.dto.planning.PreviewApplicationPlanRequest;
import com.runiversityadmisson.bot.application.dto.planning.SaveApplicationPlanRequest;
import com.runiversityadmisson.bot.application.planning.ApplicationPlanService;
import com.runiversityadmisson.bot.presentation.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** План поступления 5×5: сохранённый план, сохранение с проверкой версии и черновик автоплана. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/sessions")
public class ApplicationPlanController {
	private final ApplicationPlanService planService;

	@GetMapping("/{sessionId}/application-plan")
	public ApplicationPlanResponse getPlan(@PathVariable UUID sessionId, @AuthenticationPrincipal UUID principal) {
		ensureOwner(sessionId, principal);
		return planService.getPlan(sessionId);
	}

	@PutMapping("/{sessionId}/application-plan")
	public ApplicationPlanResponse savePlan(@PathVariable UUID sessionId, @AuthenticationPrincipal UUID principal,
			@Valid @RequestBody SaveApplicationPlanRequest request) {
		ensureOwner(sessionId, principal);
		return planService.savePlan(sessionId, request);
	}

	@PostMapping("/{sessionId}/application-plan/preview")
	public PlanPreviewResponse preview(@PathVariable UUID sessionId, @AuthenticationPrincipal UUID principal,
			@Valid @RequestBody PreviewApplicationPlanRequest request) {
		ensureOwner(sessionId, principal);
		return planService.preview(sessionId, request);
	}

	private void ensureOwner(UUID sessionId, UUID authenticatedUserId) {
		if (!sessionId.equals(authenticatedUserId)) {
			throw new ResourceNotFoundException("Заявка не найдена");
		}
	}
}
