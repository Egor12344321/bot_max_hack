package com.runiversityadmisson.bot.application.dto.response;

import java.util.List;

/** bviNote заполнен, если хотя бы на одном направлении доступно БВИ. */
public record AdmissionBenefitsResponse(
		int campaignYear,
		List<UniversityBenefitsResponse> universities,
		String bviNote
) {
}
