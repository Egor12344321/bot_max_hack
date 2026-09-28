package com.runiversityadmisson.bot.application.dto.response;

import java.util.List;

public record UniversityBenefitsResponse(
		String universityId,
		String universityName,
		int achievementPointsMax,
		List<ProgramBenefitsResponse> programs
) {
}
