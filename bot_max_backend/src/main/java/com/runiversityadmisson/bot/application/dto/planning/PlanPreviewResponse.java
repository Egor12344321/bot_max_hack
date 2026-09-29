package com.runiversityadmisson.bot.application.dto.planning;

import java.time.OffsetDateTime;
import java.util.List;

/** Черновик автоплана; ничего не сохраняется. */
public record PlanPreviewResponse(
		PlanCompositionDto composition,
		List<ProgramOptionResponse> options,
		List<PlanExplanationResponse> explanations,
		List<PlanUniversityRankingResponse> universityRanking,
		List<String> warnings,
		OffsetDateTime calculatedAt,
		int nearPreviousThreshold
) {
}
