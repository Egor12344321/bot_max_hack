package com.runiversityadmisson.bot.application.dto.olympiad;

public record OlympiadProfileResponse(
		String id,
		String profile,
		String name,
		String subjectId,
		Integer level,
		int year
) {
}
