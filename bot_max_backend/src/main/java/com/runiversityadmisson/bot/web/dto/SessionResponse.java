package com.runiversityadmisson.bot.web.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SessionResponse(UUID id, String platform, String language, String countryCode, OffsetDateTime createdAt) {
}
