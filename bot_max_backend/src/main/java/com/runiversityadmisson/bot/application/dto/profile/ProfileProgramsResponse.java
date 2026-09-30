package com.runiversityadmisson.bot.application.dto.profile;

/**
 * Сводка по рекомендациям всех выбранных направлений: сколько программ выше, около и ниже
 * прошлогоднего проходного. Вероятность поступления не считается — по одному прошлогоднему
 * проходному её честно не оценить.
 */
public record ProfileProgramsResponse(
		int total,
		int bvi,
		int abovePrevious,
		int nearPrevious,
		int belowPrevious,
		int insufficientData,
		int reserveCount,
		int realCount,
		int riskCount
) {
}
