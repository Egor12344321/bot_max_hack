package com.runiversityadmisson.bot.domain.applicant.model;

import java.util.Arrays;

/** Степень диплома олимпиады. */
public enum OlympiadDegree {
	WINNER("winner"),
	PRIZE("prize");

	private final String code;

	OlympiadDegree(String code) {
		this.code = code;
	}

	public String getCode() {
		return code;
	}

	public static OlympiadDegree fromCode(String code) {
		return Arrays.stream(values())
				.filter(degree -> degree.code.equals(code))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Неизвестная степень диплома: " + code));
	}
}
