package com.runiversityadmisson.bot.application.dto.exam;

public record EgeScoreResponse(
		String subjectId,
		String subjectName,
		Integer score,
		Integer minThreshold,
		boolean passed
) {
}
