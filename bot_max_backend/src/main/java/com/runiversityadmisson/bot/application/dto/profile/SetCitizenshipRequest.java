package com.runiversityadmisson.bot.application.dto.profile;

import jakarta.validation.constraints.NotBlank;

public record SetCitizenshipRequest(@NotBlank String countryCode) {
}
