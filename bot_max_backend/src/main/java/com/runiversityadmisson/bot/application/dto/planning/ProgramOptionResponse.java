package com.runiversityadmisson.bot.application.dto.planning;

import com.runiversityadmisson.bot.application.dto.direction.StudyDirectionResponse;
import java.util.List;

/** ProgramOption из admission-planning.openapi.yaml: вариант конкурса с персональным расчётом. */
public record ProgramOptionResponse(
		String programId,
		String programName,
		String universityId,
		String universityName,
		StudyDirectionResponse direction,
		String city,
		String campus,
		int campaignYear,
		String funding,
		String studyForm,
		String competitionType,
		String eligibility,
		boolean bviAvailable,
		Integer totalScore,
		Integer passingScorePreviousYear,
		Integer previousYear,
		Integer scoreDifference,
		String comparison,
		String dataSource,
		List<ProgramBreakdownItemResponse> breakdown,
		List<String> reasons
) {
}
