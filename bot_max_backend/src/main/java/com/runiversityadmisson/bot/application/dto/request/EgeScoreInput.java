package com.runiversityadmisson.bot.application.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EgeScoreInput(
		@NotBlank String subjectId,
		@NotNull @Min(0) @Max(100) Integer score) {
}
