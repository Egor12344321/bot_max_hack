package com.runiversityadmisson.bot.application.dto.olympiad;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record OlympiadDiplomaInput(
		@NotBlank String profileId,
		@NotBlank @Pattern(regexp = "winner|prize") String degree) {
}
