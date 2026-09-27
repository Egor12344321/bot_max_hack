package com.runiversityadmisson.bot.web;

import com.runiversityadmisson.bot.bot.session.SessionState;
import com.runiversityadmisson.bot.web.session.ApplicantSession;
import com.runiversityadmisson.bot.web.dto.EgeScoreResponse;
import com.runiversityadmisson.bot.web.dto.SessionDraftResponse;
import com.runiversityadmisson.bot.web.dto.SessionResponse;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class SessionResponseMapper {

	private static final Map<String, Subject> SUBJECTS = Map.of(
			"russian", new Subject("Русский язык", 40),
			"math-profile", new Subject("Математика (профиль)", 39),
			"informatics", new Subject("Информатика", 44),
			"physics", new Subject("Физика", 39),
			"chemistry", new Subject("Химия", 39),
			"biology", new Subject("Биология", 39),
			"social-studies", new Subject("Обществознание", 45),
			"history", new Subject("История", 35)
	);

	public SessionResponse toSession(ApplicantSession session) {
		return new SessionResponse(session.getId(), "max", session.getLanguage(), session.getCountryCode(),
				session.getCreatedAt());
	}

	public SessionDraftResponse toDraft(ApplicantSession session) {
		List<EgeScoreResponse> scores = session.getEgeScores().entrySet().stream()
				.sorted(Map.Entry.comparingByKey())
				.map(entry -> toEgeScore(entry.getKey(), entry.getValue()))
				.toList();
		return new SessionDraftResponse(session.getId(), session.getLanguage(), session.getCountryCode(), scores,
				session.getState() == SessionState.READY_FOR_MINAPP);
	}

	private EgeScoreResponse toEgeScore(String subjectId, int score) {
		Subject subject = SUBJECTS.getOrDefault(subjectId, new Subject(subjectId, 0));
		return new EgeScoreResponse(subjectId, subject.name(), score, subject.minThreshold(),
				score >= subject.minThreshold());
	}

	private record Subject(String name, int minThreshold) {
	}
}
