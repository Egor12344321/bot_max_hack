package com.runiversityadmisson.bot.application.dto.direction;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record StudyDirectionSelection(@NotNull List<@NotBlank String> directionIds) {
}
