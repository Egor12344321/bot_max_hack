package com.runiversityadmisson.bot.application.dto.benefit;

import java.util.List;

/**
 * Льготы абитуриента на одном направлении.
 * benefit — лучшая льгота: bvi, score_100 или none.
 * totalScore = egeScoreWithBenefits + achievementPoints.
 */
public record ProgramBenefitsResponse(
		String programId,
		String programName,
		String benefit,
		int egeScore,
		int egeScoreWithBenefits,
		int achievementPoints,
		int totalScore,
		Integer passingScorePreviousYear,
		List<String> missingSubjects,
		List<OlympiadBenefitResponse> olympiads,
		List<AchievementScoreResponse> achievements
) {
}
