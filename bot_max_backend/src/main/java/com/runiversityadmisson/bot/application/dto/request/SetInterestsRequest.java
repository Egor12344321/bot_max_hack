package com.runiversityadmisson.bot.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record SetInterestsRequest(
		@NotNull @Size(min = 1, max = 3) List<@NotBlank String> categoryIds) {
}
