package com.runiversityadmisson.bot.web.dto;

public record EgeScoreResponse(
		String subjectId,
		String subjectName,
		Integer score,
		Integer minThreshold,
		boolean passed
) {
}
