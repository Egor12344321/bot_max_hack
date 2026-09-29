package com.runiversityadmisson.bot.application.dto.session;

import java.time.LocalDateTime;
import java.util.UUID;

public record SessionResponse(
		UUID id,
		String platform,
		String language,
		String countryCode,
		LocalDateTime createdAt) {
}
