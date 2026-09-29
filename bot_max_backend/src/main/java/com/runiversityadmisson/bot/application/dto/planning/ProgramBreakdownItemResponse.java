package com.runiversityadmisson.bot.application.dto.planning;

/** Строка детализации расчёта. kind: exam, olympiad, achievement. */
public record ProgramBreakdownItemResponse(
		String kind,
		String name,
		Integer originalScore,
		Integer countedScore,
		boolean applied,
		String explanation
) {
}
