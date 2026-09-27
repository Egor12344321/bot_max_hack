package com.runiversityadmisson.bot.bot.service;

import com.runiversityadmisson.bot.bot.session.BotSession;
import com.runiversityadmisson.bot.bot.session.SessionService;
import com.runiversityadmisson.bot.bot.session.SessionState;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.max.botapi.model.BotStartedUpdate;
import ru.max.botapi.model.MessageCallbackUpdate;
import ru.max.botapi.model.MessageCreatedUpdate;
import ru.max.botapi.model.Update;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessUpdateService {

	private static final int MAX_EGE_SUBJECTS = 5;

	private final SessionService sessionService;
	private final MaxBotService maxBotService;
	private final BotTextService botTextService;
	private final EgeScoreParser egeScoreParser;
	private final EgeSubjectCatalog egeSubjectCatalog;

	public void process(Update update) {
		log.info("Получен вебхук: {}", update.updateType());
		switch (update) {
			case BotStartedUpdate started -> handleBotStarted(started);
			case MessageCreatedUpdate message -> handleMessage(message);
			case MessageCallbackUpdate callback -> handleCallback(callback);
			default -> log.warn("Неизвестный тип вебхука: {}", update.updateType());
		}
	}

	private void handleBotStarted(BotStartedUpdate update) {
		if (update.user() == null) {
			log.warn("Вебхук запуска без пользователя");
			return;
		}

		BotSession session = sessionService.getOrCreate(update.user().userId());
		if (session.state() == SessionState.NEW) {
			session = session.withState(SessionState.WAITING_FOR_LANGUAGE);
			sessionService.save(session);
		}
		sendCurrentQuestion(session);
	}

	private void handleMessage(MessageCreatedUpdate update) {
		if (update.message() == null || update.message().sender() == null || update.message().body() == null) {
			return;
		}

		long userId = update.message().sender().userId();
		String text = update.message().body().text();
		if (text == null || text.isBlank()) {
			return;
		}

		Optional<BotSession> optionalSession = sessionService.get(userId);
		if (optionalSession.isEmpty()) {
			return;
		}

		BotSession session = optionalSession.get();
		if (session.state() != SessionState.WAITING_FOR_EGE_SCORE) {
			sendCurrentQuestion(session);
			return;
		}

		if (session.selectedSubjectId() == null) {
			BotSession updatedSession = session.withState(SessionState.WAITING_FOR_EGE_SUBJECT);
			sessionService.save(updatedSession);
			sendCurrentQuestion(updatedSession);
			return;
		}

		egeScoreParser.parse(text).ifPresentOrElse(score -> {
			BotSession updatedSession = session.withEgeScore(session.selectedSubjectId(), score).withSelectedSubject(null);
			updatedSession = updatedSession.withState(
				updatedSession.egeScores().size() >= MAX_EGE_SUBJECTS
					? SessionState.READY_FOR_MINAPP
					: SessionState.WAITING_FOR_EGE_SUBJECT
			);
			sessionService.save(updatedSession);
			log.info(updatedSession.state() == SessionState.READY_FOR_MINAPP ? "Баллы завершены" : "Сохранён балл");
			sendCurrentQuestion(updatedSession);
		}, () -> maxBotService.sendText(userId, botTextService.invalidEge(session.language())));
	}

	private void handleCallback(MessageCallbackUpdate update) {
		if (update.callback() == null || update.callback().user() == null) {
			return;
		}

		long userId = update.callback().user().userId();
		String payload = update.callback().payload();
		if (payload == null || payload.isBlank()) {
			return;
		}

		if (update.callback().callbackId() != null) {
			maxBotService.answerCallback(update.callback().callbackId());
		}

		BotSession session = sessionService.getOrCreate(userId);
		BotSession updatedSession = transition(session, payload);
		if (updatedSession == session) {
			sendCurrentQuestion(session);
			return;
		}

		sessionService.save(updatedSession);
		sendCurrentQuestion(updatedSession);
	}

	private BotSession transition(BotSession session, String payload) {
		return switch (session.state()) {
			case NEW, WAITING_FOR_LANGUAGE -> language(payload)
				.map(value -> session.withLanguage(value).withState(SessionState.WAITING_FOR_CITIZENSHIP))
				.orElse(session);
			case WAITING_FOR_CITIZENSHIP -> citizenship(payload)
				.map(value -> session.withCitizenship(value).withState(SessionState.WAITING_FOR_TRACK))
				.orElse(session);
			case WAITING_FOR_TRACK -> track(payload)
				.map(value -> session.withTrack(value).withState(SessionState.WAITING_FOR_EGE_SUBJECT))
				.orElse(session);
			case WAITING_FOR_EGE_SUBJECT -> egeTransition(session, payload);
			case WAITING_FOR_EGE_SCORE, READY_FOR_MINAPP -> session;
		};
	}

	private BotSession egeTransition(BotSession session, String payload) {
		if ("ege_done".equals(payload) && !session.egeScores().isEmpty()) {
			log.info("Баллы завершены");
			return session.withSelectedSubject(null).withState(SessionState.READY_FOR_MINAPP);
		}
		if (!payload.startsWith("subject_") || session.egeScores().size() >= MAX_EGE_SUBJECTS) {
			return session;
		}

		String subjectId = payload.substring("subject_".length());
		if (session.egeScores().containsKey(subjectId) || egeSubjectCatalog.findById(subjectId).isEmpty()) {
			return session;
		}

		log.info("Выбран предмет");
		return session.withSelectedSubject(subjectId).withState(SessionState.WAITING_FOR_EGE_SCORE);
	}

	private Optional<String> language(String payload) {
		return switch (payload) {
			case "lang_ru" -> Optional.of("ru");
			case "lang_kk" -> Optional.of("kk");
			case "lang_ky" -> Optional.of("ky");
			default -> Optional.empty();
		};
	}

	private Optional<String> citizenship(String payload) {
		return switch (payload) {
			case "citizenship_eaeu" -> Optional.of("eaeu");
			case "citizenship_other" -> Optional.of("other");
			default -> Optional.empty();
		};
	}

	private Optional<String> track(String payload) {
		return switch (payload) {
			case "track_budget" -> Optional.of("budget");
			case "track_paid" -> Optional.of("paid");
			default -> Optional.empty();
		};
	}

	private void sendCurrentQuestion(BotSession session) {
		maxBotService.sendQuestion(session.userId(), botTextService.questionFor(session));
	}
}
