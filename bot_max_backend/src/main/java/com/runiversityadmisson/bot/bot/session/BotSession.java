package com.runiversityadmisson.bot.bot.session;

import java.util.LinkedHashMap;
import java.util.Map;

public record BotSession(
	long userId,
	SessionState state,
	String language,
	String citizenship,
	String track,
	Map<String, Integer> egeScores,
	String selectedSubjectId
) {

	public static BotSession initial(long userId) {
		return new BotSession(userId, SessionState.WAITING_FOR_LANGUAGE, "ru", null, null, Map.of(), null);
	}

	public BotSession withState(SessionState newState) {
		return new BotSession(userId, newState, language, citizenship, track, egeScores, selectedSubjectId);
	}

	public BotSession withLanguage(String newLanguage) {
		return new BotSession(userId, state, newLanguage, citizenship, track, egeScores, selectedSubjectId);
	}

	public BotSession withCitizenship(String newCitizenship) {
		return new BotSession(userId, state, language, newCitizenship, track, egeScores, selectedSubjectId);
	}

	public BotSession withTrack(String newTrack) {
		return new BotSession(userId, state, language, citizenship, newTrack, egeScores, selectedSubjectId);
	}

	public BotSession withSelectedSubject(String subjectId) {
		return new BotSession(userId, state, language, citizenship, track, egeScores, subjectId);
	}

	public BotSession withEgeScore(String subjectId, int score) {
		Map<String, Integer> scores = new LinkedHashMap<>(egeScores);
		scores.put(subjectId, score);
		return new BotSession(userId, state, language, citizenship, track, Map.copyOf(scores), selectedSubjectId);
	}
}
