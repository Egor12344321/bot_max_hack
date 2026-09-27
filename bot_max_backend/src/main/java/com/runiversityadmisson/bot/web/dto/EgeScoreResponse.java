package com.runiversityadmisson.bot.web.dto;

public record EgeScoreResponse(String subjectId, String subjectName, int score, int minThreshold,
		boolean passed) {
}
