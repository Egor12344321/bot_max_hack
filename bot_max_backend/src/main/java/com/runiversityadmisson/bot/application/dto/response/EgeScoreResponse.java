package com.runiversityadmisson.bot.application.dto.response;

public record EgeScoreResponse(
		String subjectId,
		String subjectName,
		Integer score,
		Integer minThreshold,
		boolean passed
) {
}
