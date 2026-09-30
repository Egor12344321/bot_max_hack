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
		List<String> reasons,
		Integer seats,
		int entranceScoreMax,
		List<String> availableCompetitionTypes
) {
	public ProgramOptionResponse(String programId, String programName, String universityId, String universityName,
			StudyDirectionResponse direction, String city, String campus, int campaignYear, String funding,
			String studyForm, String competitionType, String eligibility, boolean bviAvailable, Integer totalScore,
			Integer passingScorePreviousYear, Integer previousYear, Integer scoreDifference, String comparison,
			String dataSource, List<ProgramBreakdownItemResponse> breakdown, List<String> reasons) {
		this(programId, programName, universityId, universityName, direction, city, campus, campaignYear, funding,
				studyForm, competitionType, eligibility, bviAvailable, totalScore, passingScorePreviousYear, previousYear,
				scoreDifference, comparison, dataSource, breakdown, reasons, null, 300, List.of(competitionType));
	}

	@com.fasterxml.jackson.annotation.JsonProperty("riskStatus")
	public String riskStatus() {
		if (bviAvailable) return "bvi";
		if (!"eligible".equals(eligibility) || scoreDifference == null) return "unknown";
		return scoreDifference >= 20 ? "reserve" : scoreDifference >= 0 ? "real" : "risk";
	}
}
