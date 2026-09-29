package com.runiversityadmisson.bot.application.dto.planning;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** Состав плана: порядок вузов и программ задаёт приоритеты. bviProgramId — одно место использования БВИ. */
public record PlanCompositionDto(
		@NotNull List<@Valid PlanUniversityDto> universities,
		String bviProgramId
) {
}
