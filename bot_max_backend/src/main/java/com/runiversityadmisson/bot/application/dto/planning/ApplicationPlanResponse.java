package com.runiversityadmisson.bot.application.dto.planning;

import java.time.OffsetDateTime;
import java.util.List;

/** Сохранённый план с актуальным расчётом. version 0 и пустой состав — план ещё не сохраняли. */
public record ApplicationPlanResponse(
		PlanCompositionDto composition,
		List<ProgramOptionResponse> options,
		List<PlanExplanationResponse> explanations,
		List<PlanUniversityRankingResponse> universityRanking,
		List<String> warnings,
		OffsetDateTime calculatedAt,
		int nearPreviousThreshold,
		int version,
		OffsetDateTime savedAt
) {
}
