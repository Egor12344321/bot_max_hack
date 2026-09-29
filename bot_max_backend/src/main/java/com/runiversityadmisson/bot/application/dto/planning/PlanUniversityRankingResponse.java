package com.runiversityadmisson.bot.application.dto.planning;

import java.util.List;

/**
 * Почему вуз в автоплане: попадания в топ-3 по направлениям и сводные показатели.
 * addedAsFill — добавлен при доборе, retained — из плана пользователя.
 */
public record PlanUniversityRankingResponse(
		String universityId,
		String universityName,
		int hits,
		int sumOfPlaces,
		int firstPlaces,
		List<PlanTopPlaceResponse> places,
		boolean addedAsFill,
		boolean retained,
		String message
) {
}
