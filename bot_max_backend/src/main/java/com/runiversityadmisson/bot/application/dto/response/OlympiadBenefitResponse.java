package com.runiversityadmisson.bot.application.dto.response;

/** Что конкретный диплом даёт на направлении. benefit: bvi, score_100, achievement_points, none. */
public record OlympiadBenefitResponse(
		String profileId,
		String olympiadName,
		String profileName,
		Integer level,
		String degree,
		String benefit,
		Integer points,
		String note
) {
}
