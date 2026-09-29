package com.runiversityadmisson.bot.presentation.controller.benefit;

import com.runiversityadmisson.bot.application.dto.benefit.AdmissionBenefitsResponse;
import com.runiversityadmisson.bot.application.benefit.AdmissionBenefitService;
import com.runiversityadmisson.bot.presentation.exception.ResourceNotFoundException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Что дипломы олимпиад и ИД дают пользователю в каждом вузе и на каждом направлении. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/sessions")
public class AdmissionBenefitsController {

	private final AdmissionBenefitService admissionBenefitService;

	@GetMapping("/{sessionId}/admission-benefits")
	public AdmissionBenefitsResponse getAdmissionBenefits(
			@PathVariable UUID sessionId,
			@AuthenticationPrincipal UUID authenticatedUserId,
			@RequestParam(required = false) String universityId) {
		if (!sessionId.equals(authenticatedUserId)) {
			throw new ResourceNotFoundException("Заявка не найдена");
		}
		return admissionBenefitService.getBenefits(sessionId, universityId);
	}
}
