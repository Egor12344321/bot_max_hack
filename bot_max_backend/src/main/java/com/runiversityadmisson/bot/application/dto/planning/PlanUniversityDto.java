package com.runiversityadmisson.bot.application.dto.planning;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** Вуз плана и его программы в порядке приоритета. */
public record PlanUniversityDto(
		@NotBlank String universityId,
		@NotNull List<@NotBlank String> programIds
) {
}
