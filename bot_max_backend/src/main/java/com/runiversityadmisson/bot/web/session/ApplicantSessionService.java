package com.runiversityadmisson.bot.web.session;

import com.runiversityadmisson.bot.bot.session.Session;
import com.runiversityadmisson.bot.bot.session.SessionState;
import com.runiversityadmisson.bot.web.repository.ApplicantSessionRepository;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ApplicantSessionService {

	private final ApplicantSessionRepository applicantSessionRepository;

	public ApplicantSessionService(ApplicantSessionRepository applicantSessionRepository) {
		this.applicantSessionRepository = applicantSessionRepository;
	}

	@Transactional
	public void saveCompletedBotSession(Session botSession) {
		if (botSession.getState() != SessionState.READY_FOR_MINAPP) {
			throw new IllegalStateException("Бот-онбординг не завершён");
		}

		OffsetDateTime now = OffsetDateTime.now();
		ApplicantSession session = applicantSessionRepository.findByMaxUserId(botSession.getUserId())
				.orElseGet(() -> newSession(botSession.getUserId(), now));
		session.setLanguage(botSession.getLanguage());
		session.setCountryCode(botSession.getCitizenship());
		session.setTrack(botSession.getTrack());
		session.setState(botSession.getState());
		session.setEgeScores(new HashMap<>(botSession.getEgeScores()));
		session.setUpdatedAt(now);
		applicantSessionRepository.save(session);
	}

	@Transactional(readOnly = true)
	public ApplicantSession getCompletedByMaxUserId(Long maxUserId) {
		ApplicantSession session = applicantSessionRepository.findByMaxUserId(maxUserId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Сессия не найдена"));
		if (session.getState() != SessionState.READY_FOR_MINAPP) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Бот-онбординг не завершён");
		}
		return session;
	}

	@Transactional(readOnly = true)
	public ApplicantSession getById(UUID id) {
		return applicantSessionRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Сессия не найдена"));
	}

	@Transactional
	public void deleteByMaxUserId(Long maxUserId) {
		applicantSessionRepository.deleteByMaxUserId(maxUserId);
	}

	private ApplicantSession newSession(Long maxUserId, OffsetDateTime now) {
		ApplicantSession session = new ApplicantSession();
		session.setId(UUID.randomUUID());
		session.setMaxUserId(maxUserId);
		session.setCreatedAt(now);
		return session;
	}
}
