package com.runiversityadmisson.bot.application.dto.response;

import java.util.List;

public record PrivilegeCategoryResponse(
		String id,
		String name,
		String quotaType,
		Integer maxQuotaPercent,
		List<String> requiredDocuments
) {
}
