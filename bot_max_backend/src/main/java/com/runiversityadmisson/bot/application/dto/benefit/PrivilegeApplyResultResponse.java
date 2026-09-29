package com.runiversityadmisson.bot.application.dto.benefit;

import java.util.List;

public record PrivilegeApplyResultResponse(
		List<String> selectedCategoryIds,
		String bestQuotaType,
		String message
) {
}
