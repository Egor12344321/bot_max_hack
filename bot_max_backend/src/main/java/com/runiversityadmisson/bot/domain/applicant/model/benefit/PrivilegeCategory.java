package com.runiversityadmisson.bot.domain.applicant.model.benefit;

import java.util.List;

/**
 * Льготная категория из справочника.
 * Пока справочник тестовый и читается из classpath:catalog/privilege-categories.json.
 */
public record PrivilegeCategory(
		String id,
		String name,
		QuotaType quotaType,
		Integer maxQuotaPercent,
		List<String> requiredDocuments
) {
}
