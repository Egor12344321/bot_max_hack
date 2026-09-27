package com.runiversityadmisson.bot.bot.service;

import com.runiversityadmisson.bot.bot.client.MaxBotClient;
import com.runiversityadmisson.bot.bot.client.dto.NewMessageBody;
import com.runiversityadmisson.bot.bot.session.Session;
import com.runiversityadmisson.bot.bot.session.SessionService;
import com.runiversityadmisson.bot.bot.session.SessionState;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class OnboardingService {

	private static final Set<String> EAEU_COUNTRIES = Set.of("BY", "KZ", "KG", "AM");

	private final MaxBotClient maxBotClient;
	private final SessionService sessionService;
	private final MessageSource messageSource;
	private final String webApp;

	public OnboardingService(MaxBotClient maxBotClient, SessionService sessionService,
			MessageSource messageSource, @Value("${max.bot.web-app}") String webApp) {
		this.maxBotClient = maxBotClient;
		this.sessionService = sessionService;
		this.messageSource = messageSource;
		this.webApp = webApp;
	}

	public void start(Long userId) {
		Session session = sessionService.getOrCreate(userId);
		if (session.getState() != SessionState.NEW) {
			log.debug("Сессия {} уже запущена на этапе {}", userId, session.getState());
			return;
		}
		moveTo(session, SessionState.WAITING_FOR_LANGUAGE);
		sendLanguageQuestion(userId);
	}

	public void handleCallback(Long userId, String callbackId, String payload) {
		Session session = sessionService.getOrCreate(userId);

		if (callbackId != null) {
			maxBotClient.answerCallback(callbackId, msg("button.accepted", session.getLanguage()));
		}
		if (payload == null || payload.isBlank()) {
			log.warn("Callback от {} без payload", userId);
			return;
		}

		if (payload.startsWith("lang_")) {
			handleLanguage(session, payload.substring(5));
		} else if (payload.startsWith("citizenship_")) {
			handleCitizenship(session, payload.substring(12));
		} else if ("ege_more".equals(payload)) {
			moveTo(session, SessionState.WAITING_FOR_EGE_SUBJECT);
			sendSubjectQuestion(userId, session.getLanguage());
		} else if ("ege_done".equals(payload)) {
			finishEge(session);
		} else if (payload.startsWith("subject_")) {
			session.setCurrentSubject(payload.substring(8));
			moveTo(session, SessionState.WAITING_FOR_EGE_SCORE);
			maxBotClient.sendMessage(userId, NewMessageBody.builder()
					.text(msg("ege.ask.score", session.getLanguage()))
					.build());
		} else {
			log.warn("Неизвестный payload у сессии {}: {}", userId, payload);
		}
	}

	public void handleText(Long userId, String text) {
		Session session = sessionService.getOrCreate(userId);

		if (session.getState() != SessionState.WAITING_FOR_EGE_SCORE) {
			maxBotClient.sendMessage(userId, NewMessageBody.builder()
					.text(msg("choose.button", session.getLanguage()))
					.build());
			return;
		}
		if (text == null || text.isBlank()) {
			maxBotClient.sendMessage(userId, NewMessageBody.builder()
					.text(msg("ege.not.number", session.getLanguage()))
					.build());
			return;
		}

		Integer score;
		try {
			score = Integer.valueOf(text.trim());
		} catch (NumberFormatException exception) {
			maxBotClient.sendMessage(userId, NewMessageBody.builder()
					.text(msg("ege.not.number", session.getLanguage()))
					.build());
			return;
		}
		if (score < 0 || score > 100) {
			log.debug("Балл вне диапазона у сессии {}: {}", userId, score);
			maxBotClient.sendMessage(userId, NewMessageBody.builder()
					.text(msg("ege.invalid.score", session.getLanguage()))
					.build());
			return;
		}

		String subject = session.getCurrentSubject();
		session.getEgeScores().put(subject, score);
		log.debug("Сессия {}: балл {} по предмету {}", userId, score, subject);
		moveTo(session, SessionState.WAITING_FOR_EGE_MORE);
		sendMoreSubjectsQuestion(userId, session.getLanguage());
	}

	private void handleLanguage(Session session, String language) {
		session.setLanguage(language);
		moveTo(session, SessionState.WAITING_FOR_CITIZENSHIP);
		sendCitizenshipQuestion(session.getUserId(), language);
	}

	private void handleCitizenship(Session session, String country) {
		session.setCitizenship(country);
		String lang = session.getLanguage();
		log.info("Сессия {}: гражданство={}", session.getUserId(), country);

		if (EAEU_COUNTRIES.contains(country)) {
			session.setTrack("domestic_equivalent");
			moveTo(session, SessionState.WAITING_FOR_EGE_SUBJECT);
			maxBotClient.sendMessage(session.getUserId(), NewMessageBody.builder()
					.text(msg("track.eaeu", lang))
					.build());
			sendSubjectQuestion(session.getUserId(), lang);
		} else {
			session.setTrack("rf_quota_or_paid");
			moveTo(session, SessionState.READY_FOR_MINAPP);
			maxBotClient.sendMessage(session.getUserId(), NewMessageBody.builder()
					.text(msg("track.foreigner", lang))
					.attachment(openMiniAppButton(lang, session.getUserId()))
					.build());
		}
	}

	private void finishEge(Session session) {
		String lang = session.getLanguage();
		log.info("Сессия {} завершила ввод баллов: {}", session.getUserId(), session.getEgeScores().size());
		moveTo(session, SessionState.READY_FOR_MINAPP);
		maxBotClient.sendMessage(session.getUserId(), NewMessageBody.builder()
				.text(msg("ege.done", lang))
				.attachment(openMiniAppButton(lang, session.getUserId()))
				.build());
	}

	private NewMessageBody.Attachment openMiniAppButton(String lang, Long userId) {
		return NewMessageBody.Attachment.inlineKeyboard(List.of(
				List.of(NewMessageBody.Button.openApp(
						msg("button.open.app", lang),
						"user_" + userId, webApp
				))
		));
	}

	private void sendLanguageQuestion(Long userId) {
		maxBotClient.sendMessage(userId, NewMessageBody.builder()
				.text(msg("greeting", "ru"))
				.attachment(NewMessageBody.Attachment.inlineKeyboard(List.of(
						List.of(
								NewMessageBody.Button.callback(msg("language.ru", "ru"), "lang_ru"),
								NewMessageBody.Button.callback(msg("language.kk", "kk"), "lang_kk")
						),
						List.of(
								NewMessageBody.Button.callback(msg("language.ky", "ky"), "lang_ky")
						)
				)))
				.build());
	}

	private void sendCitizenshipQuestion(Long userId, String lang) {
		maxBotClient.sendMessage(userId, NewMessageBody.builder()
				.text(msg("ask.citizenship", lang))
				.attachment(NewMessageBody.Attachment.inlineKeyboard(List.of(
						List.of(
								NewMessageBody.Button.callback(msg("citizenship.BY", lang), "citizenship_BY"),
								NewMessageBody.Button.callback(msg("citizenship.KZ", lang), "citizenship_KZ")
						),
						List.of(
								NewMessageBody.Button.callback(msg("citizenship.KG", lang), "citizenship_KG"),
								NewMessageBody.Button.callback(msg("citizenship.AM", lang), "citizenship_AM")
						),
						List.of(
								NewMessageBody.Button.callback(msg("citizenship.OTHER", lang), "citizenship_OTHER")
						)
				)))
				.build());
	}

	private void sendSubjectQuestion(Long userId, String lang) {
		maxBotClient.sendMessage(userId, NewMessageBody.builder()
				.text(msg("ege.ask.subject", lang))
				.attachment(NewMessageBody.Attachment.inlineKeyboard(List.of(
						List.of(
								NewMessageBody.Button.callback(msg("subject.russian", lang), "subject_russian"),
								NewMessageBody.Button.callback(msg("subject.math-profile", lang), "subject_math-profile")
						),
						List.of(
								NewMessageBody.Button.callback(msg("subject.informatics", lang), "subject_informatics"),
								NewMessageBody.Button.callback(msg("subject.physics", lang), "subject_physics")
						),
						List.of(
								NewMessageBody.Button.callback(msg("subject.chemistry", lang), "subject_chemistry"),
								NewMessageBody.Button.callback(msg("subject.biology", lang), "subject_biology")
						),
						List.of(
								NewMessageBody.Button.callback(msg("subject.social-studies", lang), "subject_social-studies"),
								NewMessageBody.Button.callback(msg("subject.history", lang), "subject_history")
						)
				)))
				.build());
	}

	private void sendMoreSubjectsQuestion(Long userId, String lang) {
		maxBotClient.sendMessage(userId, NewMessageBody.builder()
				.text(msg("ege.more", lang))
				.attachment(NewMessageBody.Attachment.inlineKeyboard(List.of(
						List.of(
								NewMessageBody.Button.callback(msg("button.add", lang), "ege_more"),
								NewMessageBody.Button.callback(msg("button.done", lang), "ege_done")
						)
				)))
				.build());
	}

	private void moveTo(Session session, SessionState state) {
		log.debug("Сессия {}: состояние {} -> {}", session.getUserId(), session.getState(), state);
		session.setState(state);
		sessionService.save(session);
	}

	private String msg(String key, String lang) {
		Locale locale = switch (lang == null ? "ru" : lang) {
			case "kk" -> Locale.forLanguageTag("kk");
			case "ky" -> Locale.forLanguageTag("ky");
			default -> Locale.forLanguageTag("ru");
		};
		return messageSource.getMessage(key, null, locale);
	}
}
