package com.runiversityadmisson.bot.application.dto.request;

import jakarta.validation.constraints.NotBlank;

public record SetLanguageRequest(@NotBlank String language) {
}
