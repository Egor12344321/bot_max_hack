package com.runiversityadmisson.bot.application.max.onboarding;

import com.runiversityadmisson.bot.application.max.dialog.BotQuestionnaire;
import com.runiversityadmisson.bot.application.max.dialog.BotQuestionnaireStore;
import com.runiversityadmisson.bot.application.max.dialog.BotQuestionnaireStep;
import com.runiversityadmisson.bot.domain.applicant.service.UserService;
import com.runiversityadmisson.bot.domain.applicant.ports.exam.SubjectRepository;
import com.runiversityadmisson.bot.infrastructure.external.max.MaxBotClient;
import java.util.Locale;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class OnboardingConversation {

	private static final Set<String> EAEU_COUNTRIES = Set.of("BY", "KZ", "KG", "AM");

	/** Язык и гражданство, которые ставятся, когда эти шаги выключены. */
	static final String DEFAULT_LANGUAGE = "ru";
	static final String DEFAULT_CITIZENSHIP = "RU";

	private final MaxBotClient maxBotClient;
	private final BotQuestionnaireStore sessionService;
	private final OnboardingMessenger botMessageService;
	private final UserService userService;
	private final boolean askLanguageAndCitizenship;
	private final SubjectRepository subjects;

	/** Полный диалог: язык → гражданство → ЕГЭ. */
	public OnboardingConversation(MaxBotClient maxBotClient,
							 BotQuestionnaireStore sessionService,
							 OnboardingMessenger botMessageService,
							 UserService userService, SubjectRepository subjects) {
		this(maxBotClient, sessionService, botMessageService, userService, true, subjects);
	}

	/**
	 * @param askLanguageAndCitizenship флаг bot.onboarding.ask-language-and-citizenship: false — сразу к ЕГЭ,
	 *                                  язык русский, гражданство РФ
	 */
	@Autowired
	public OnboardingConversation(MaxBotClient maxBotClient,
							 BotQuestionnaireStore sessionService,
							 OnboardingMessenger botMessageService,
							 UserService userService,
							 @Value("${bot.onboarding.ask-language-and-citizenship:false}") boolean askLanguageAndCitizenship,
                             SubjectRepository subjects) {
		this.maxBotClient = maxBotClient;
		this.sessionService = sessionService;
		this.botMessageService = botMessageService;
		this.userService = userService;
		this.askLanguageAndCitizenship = askLanguageAndCitizenship;
		this.subjects = subjects;
	}

	/**
	 * Пользователь запустил бота (bot_started): незаконченная анкета в Redis сбрасывается,
	 * диалог начинается заново. Сохранённая заявка в БД не трогается — её удаляет только /restart.
	 */
	public void restartFromBotStart(Long userId) {
		sessionService.delete(userId);
		start(userId);
	}

	public void start(Long userId) {
		BotQuestionnaire session = sessionService.getOrCreate(userId);
		if (session.getState() != BotQuestionnaireStep.NEW) {
			log.debug("Сессия {} уже запущена на этапе {}", userId, session.getState());
			return;
		}
		if (!askLanguageAndCitizenship) {
			session.setLanguage(DEFAULT_LANGUAGE);
			session.setCitizenship(DEFAULT_CITIZENSHIP);
			session.setTrack("domestic_equivalent");
			moveTo(session, BotQuestionnaireStep.WAITING_FOR_EGE_SUBJECT);
			botMessageService.sendEgeGreeting(userId, DEFAULT_LANGUAGE);
			botMessageService.sendSubjectQuestion(userId, DEFAULT_LANGUAGE);
			return;
		}
		moveTo(session, BotQuestionnaireStep.WAITING_FOR_LANGUAGE);
		botMessageService.sendLanguageQuestion(userId);
	}

	public void handleCallback(Long userId, String callbackId, String payload) {
		BotQuestionnaire session = sessionService.getOrCreate(userId);

		if (callbackId != null) {
			maxBotClient.answerCallback(callbackId, "Принято");
		}
		if (payload == null || payload.isBlank()) {
			log.warn("Callback от {} без payload", userId);
			return;
		}

		boolean allowed = payload.startsWith("lang_") && session.getState() == BotQuestionnaireStep.WAITING_FOR_LANGUAGE
				&& Set.of("ru", "kk", "ky").contains(payload.substring(5))
				|| payload.startsWith("citizenship_") && session.getState() == BotQuestionnaireStep.WAITING_FOR_CITIZENSHIP
				&& Set.of("RU", "BY", "KZ", "KG", "AM", "OTHER").contains(payload.substring(12))
				|| Set.of("ege_more", "ege_done").contains(payload) && session.getState() == BotQuestionnaireStep.WAITING_FOR_EGE_MORE
				|| payload.startsWith("subject_") && (session.getState() == BotQuestionnaireStep.WAITING_FOR_EGE_SUBJECT
						|| session.getState() == BotQuestionnaireStep.WAITING_FOR_EGE_SCORE)
				&& subjects.existsById(payload.substring(8));
		if (!allowed) {
			botMessageService.sendChooseButtonHint(userId, session.getLanguage());
			return;
		}

		if (payload.startsWith("lang_")) {
			handleLanguage(session, payload.substring(5));
		} else if (payload.startsWith("citizenship_")) {
			handleCitizenship(session, payload.substring(12));
		} else if ("ege_more".equals(payload)) {
			moveTo(session, BotQuestionnaireStep.WAITING_FOR_EGE_SUBJECT);
			botMessageService.sendSubjectQuestion(userId, session.getLanguage());
		} else if ("ege_done".equals(payload)) {
			finishEge(session);
		} else if (payload.startsWith("subject_")) {
			session.setCurrentSubject(payload.substring(8));
			moveTo(session, BotQuestionnaireStep.WAITING_FOR_EGE_SCORE);
			botMessageService.sendEgeScoreQuestion(userId, session.getLanguage());
		} else {
			log.warn("Неизвестный payload у сессии {}: {}", userId, payload);
		}
	}

	public void handleText(Long userId, String text) {
		if (isRestartCommand(text)) {
			log.info("Пользователь {} запросил перезапуск онбординга", userId);
			sessionService.delete(userId);
			userService.deleteByMaxUserId(userId);
			start(userId);
			return;
		}
		BotQuestionnaire session = sessionService.getOrCreate(userId);

		if (session.getState() != BotQuestionnaireStep.WAITING_FOR_EGE_SCORE) {
			botMessageService.sendChooseButtonHint(userId, session.getLanguage());
			return;
		}

		Integer score = parseScore(text);
		if (score == null) {
			log.debug("Сессия {}: не распознан балл «{}»", userId, text);
			botMessageService.sendInvalidScore(userId, session.getLanguage());
			return;
		}

		String subject = session.getCurrentSubject();
		var definition = subjects.findById(subject).orElse(null);
		if (definition == null) {
			moveTo(session, BotQuestionnaireStep.WAITING_FOR_EGE_SUBJECT);
			botMessageService.sendSubjectQuestion(userId, session.getLanguage());
			return;
		}
		if (score < definition.getMinThreshold()) {
			botMessageService.sendBelowMinimum(userId, session.getLanguage(), definition.getMinThreshold());
			return;
		}
		session.getEgeScores().put(subject, score);
		log.debug("Сессия {}: балл {} по предмету {}", userId, score, subject);
		moveTo(session, BotQuestionnaireStep.WAITING_FOR_EGE_MORE);
		botMessageService.sendMoreSubjectsQuestion(userId, session.getLanguage());
	}

	private Integer parseScore(String text) {
		if (text == null || text.isBlank()) {
			return null;
		}
		try {
			int score = Integer.parseInt(text.trim());
			return score >= 0 && score <= 100 ? score : null;
		} catch (NumberFormatException exception) {
			return null;
		}
	}

	private boolean isRestartCommand(String text) {
		if (text == null) {
			return false;
		}
		String command = text.trim().split("\\s+", 2)[0].toLowerCase(Locale.ROOT);
		return command.equals("/restart") || command.startsWith("/restart@");
	}

	private void handleLanguage(BotQuestionnaire session, String language) {
		session.setLanguage(language);
		moveTo(session, BotQuestionnaireStep.WAITING_FOR_CITIZENSHIP);
		botMessageService.sendCitizenshipQuestion(session.getUserId(), language);
	}

	private void handleCitizenship(BotQuestionnaire session, String country) {
		session.setCitizenship(country);
		String lang = session.getLanguage();
		log.info("Сессия {}: гражданство={}", session.getUserId(), country);

		if ("RU".equals(country) || EAEU_COUNTRIES.contains(country)) {
			session.setTrack("domestic_equivalent");
			moveTo(session, BotQuestionnaireStep.WAITING_FOR_EGE_SUBJECT);
			botMessageService.sendTrackMessage(session.getUserId(), lang,
					"RU".equals(country) ? "track.russia" : "track.eaeu");
			botMessageService.sendSubjectQuestion(session.getUserId(), lang);
		} else {
			session.setTrack("rf_quota_or_paid");
			moveTo(session, BotQuestionnaireStep.READY_FOR_MINAPP);
			completeOnboarding(session);
			botMessageService.sendForeignerInfo(session.getUserId(), lang);
		}
	}

	private void finishEge(BotQuestionnaire session) {
		String lang = session.getLanguage();
		if (session.getEgeScores().size() < 3 || !session.getEgeScores().containsKey("russian")) {
			botMessageService.sendIncompleteEge(session.getUserId(), lang);
			return;
		}
		for (var entry : session.getEgeScores().entrySet()) {
			var subject = subjects.findById(entry.getKey()).orElse(null);
			if (subject == null || entry.getValue() == null || entry.getValue() < subject.getMinThreshold() || entry.getValue() > 100) {
				botMessageService.sendIncompleteEge(session.getUserId(), lang);
				return;
			}
		}
		log.info("Сессия {} завершила ввод баллов: {}", session.getUserId(), session.getEgeScores().size());
		moveTo(session, BotQuestionnaireStep.READY_FOR_MINAPP);
		completeOnboarding(session);
		botMessageService.sendEgeDone(session.getUserId(), lang);
	}

	private void completeOnboarding(BotQuestionnaire session) {
		userService.syncFromBot(
				session.getUserId(),
				session.getLanguage(),
				session.getCitizenship(),
				session.getTrack(),
				session.getEgeScores()
		);
		sessionService.delete(session.getUserId());
	}

	private void moveTo(BotQuestionnaire session, BotQuestionnaireStep state) {
		log.debug("Сессия {}: состояние {} -> {}", session.getUserId(), session.getState(), state);
		session.setState(state);
		sessionService.save(session);
	}
}
