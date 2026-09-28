package com.runiversityadmisson.bot.domain.applicant.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;

/**
 * Тип квоты. Порядок констант = приоритет выгоды: чем выше в списке, тем выгоднее.
 */
public enum QuotaType {
	BVI("bvi"),
	SPECIAL_QUOTA("special_quota"),
	SEPARATE_QUOTA("separate_quota"),
	TARGET_QUOTA("target_quota"),
	NONE("none");

	private final String code;

	QuotaType(String code) {
		this.code = code;
	}

	@JsonValue
	public String getCode() {
		return code;
	}

	@JsonCreator
	public static QuotaType fromCode(String code) {
		return Arrays.stream(values())
				.filter(type -> type.code.equals(code))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Неизвестный тип квоты: " + code));
	}

	/** Наиболее выгодный тип квоты из переданных; если список пуст — NONE. */
	public static QuotaType best(Collection<QuotaType> types) {
		return types.stream()
				.min(Comparator.naturalOrder())
				.orElse(NONE);
	}
}
