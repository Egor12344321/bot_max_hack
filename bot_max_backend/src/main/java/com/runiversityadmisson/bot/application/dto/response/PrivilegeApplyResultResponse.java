package com.runiversityadmisson.bot.application.dto.response;

import java.util.List;

public record PrivilegeApplyResultResponse(
		List<String> selectedCategoryIds,
		String bestQuotaType,
		String message
) {
}
