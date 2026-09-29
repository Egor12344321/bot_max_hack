package com.runiversityadmisson.bot.application.dto.planning;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/**
 * mode: generate — план с нуля, fill — дополнить basePlan.
 * allowedDeficit: насколько итоговый балл может быть ниже прошлогоднего проходного (0, 10, 15, 20; по умолчанию 15).
 */
public record PreviewApplicationPlanRequest(
		@NotBlank String mode,
		@Valid PlanCompositionDto basePlan,
		Integer allowedDeficit
) {
}
