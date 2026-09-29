package com.runiversityadmisson.bot.application.dto.olympiad;

import java.util.List;

public record OlympiadResponse(
		String id,
		String name,
		boolean isVsosh,
		Integer listNumber,
		List<OlympiadProfileResponse> profiles
) {
}
