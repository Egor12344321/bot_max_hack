package com.runiversityadmisson.bot.application.dto.benefit;

/** Совпадает с AchievementScoreBreakdownItem из OpenAPI. */
public record AchievementScoreResponse(
		String achievementId,
		String achievementName,
		int points,
		boolean counted,
		String note
) {
}
