package com.runiversityadmisson.bot.application.dto.session;

import com.runiversityadmisson.bot.application.dto.exam.EgeScoreResponse;

import java.util.List;
import java.util.UUID;

public record SessionDraftResponse(
		UUID id,
		String language,
		String countryCode,
		List<EgeScoreResponse> egeScores,
		boolean isCompleteFromBot
) {
}
