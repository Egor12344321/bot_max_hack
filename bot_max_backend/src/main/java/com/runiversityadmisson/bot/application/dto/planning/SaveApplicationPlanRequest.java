package com.runiversityadmisson.bot.application.dto.planning;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** PUT: полный состав плана и версия, от которой пользователь начинал правки. */
public record SaveApplicationPlanRequest(
		@NotNull List<@Valid PlanUniversityDto> universities,
		String bviProgramId,
		@NotNull @Min(0) Integer expectedVersion
) {
}
