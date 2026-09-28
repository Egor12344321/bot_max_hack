package com.runiversityadmisson.bot.application.dto.request;

import jakarta.validation.constraints.NotBlank;

public record SetCitizenshipRequest(@NotBlank String countryCode) {
}
