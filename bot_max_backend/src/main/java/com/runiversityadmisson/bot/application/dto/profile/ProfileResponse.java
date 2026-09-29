package com.runiversityadmisson.bot.application.dto.profile;

import com.runiversityadmisson.bot.application.dto.direction.InterestCategoryResponse;
import com.runiversityadmisson.bot.application.dto.direction.StudyDirectionResponse;
import com.runiversityadmisson.bot.application.dto.exam.EgeScoreResponse;
import com.runiversityadmisson.bot.application.dto.olympiad.OlympiadDiplomaResponse;
import java.util.List;

/**
 * Экран «Профиль»: всё, что пользователь указал, и сводка по его подборкам.
 * Имя берётся на фронте из MAX и на бэкенде не хранится.
 */
public record ProfileResponse(
		String language,
		String countryCode,
		List<EgeScoreResponse> egeScores,
		int egeTotal,
		List<InterestCategoryResponse> interests,
		List<StudyDirectionResponse> directions,
		List<OlympiadDiplomaResponse> olympiads,
		List<String> achievements,
		List<String> privileges,
		ProfileProgramsResponse programs,
		String advice
) {
}
