package com.runiversityadmisson.bot.application.dto.response;

public record OlympiadDiplomaResponse(
		String profileId,
		String olympiadId,
		String olympiadName,
		String profileName,
		Integer level,
		String degree,
		int year
) {
}
