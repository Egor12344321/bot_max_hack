package com.runiversityadmisson.bot.bot.service;

import com.runiversityadmisson.bot.bot.client.MaxBotClient;
import com.runiversityadmisson.bot.bot.client.dto.NewMessageBody;
import com.runiversityadmisson.bot.bot.session.Session;
import com.runiversityadmisson.bot.bot.session.SessionService;
import com.runiversityadmisson.bot.bot.session.SessionState;
import com.runiversityadmisson.bot.web.session.ApplicantSessionService;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class OnboardingServiceTest {

	private static final Long USER_ID = 4242L;

	@Test
	void validEgeScoreIsSavedAndAdvancesSession() {
		Session session = sessionWaitingForScore();
		SessionService sessionService = mock(SessionService.class);
		MaxBotClient maxBotClient = mock(MaxBotClient.class);
		StaticMessageSource messageSource = messageSource();
		when(sessionService.getOrCreate(USER_ID)).thenReturn(session);

		new OnboardingService(maxBotClient, sessionService, messageSource, "test_bot", mock(ApplicantSessionService.class)).handleText(USER_ID, "88");

		assertThat(session.getEgeScores()).containsEntry("math-profile", 88);
		assertThat(session.getState()).isEqualTo(SessionState.WAITING_FOR_EGE_MORE);
		verify(sessionService).save(session);
		verify(maxBotClient).sendMessage(eq(USER_ID), any(NewMessageBody.class));
	}

	@Test
	void scoreAboveRangeDoesNotChangeSessionAndAsksAgain() {
		Session session = sessionWaitingForScore();
		SessionService sessionService = mock(SessionService.class);
		MaxBotClient maxBotClient = mock(MaxBotClient.class);
		StaticMessageSource messageSource = messageSource();
		when(sessionService.getOrCreate(USER_ID)).thenReturn(session);

		new OnboardingService(maxBotClient, sessionService, messageSource, "test_bot", mock(ApplicantSessionService.class)).handleText(USER_ID, "101");

		assertThat(session.getEgeScores()).isEmpty();
		assertThat(session.getState()).isEqualTo(SessionState.WAITING_FOR_EGE_SCORE);
		assertThat(session.getCurrentSubject()).isEqualTo("math-profile");
		assertThat(lastMessageText(maxBotClient)).isEqualTo("invalid");
		verify(sessionService, never()).save(session);
	}

	@Test
	void notANumberAsksForNumberAgainAndKeepsWaiting() {
		Session session = sessionWaitingForScore();
		SessionService sessionService = mock(SessionService.class);
		MaxBotClient maxBotClient = mock(MaxBotClient.class);
		StaticMessageSource messageSource = messageSource();
		when(sessionService.getOrCreate(USER_ID)).thenReturn(session);
		OnboardingService service = new OnboardingService(maxBotClient, sessionService, messageSource, "test_bot", mock(ApplicantSessionService.class));

		service.handleText(USER_ID, "а сколько максимум?");
		service.handleText(USER_ID, "80");

		assertThat(session.getEgeScores()).containsEntry("math-profile", 80);
		assertThat(session.getState()).isEqualTo(SessionState.WAITING_FOR_EGE_MORE);
	}

	@Test
	void repeatedBotStartPreservesExistingOnboardingProgress() {
		Session session = sessionWaitingForScore();
		session.getEgeScores().put("russian", 91);
		SessionService sessionService = mock(SessionService.class);
		MaxBotClient maxBotClient = mock(MaxBotClient.class);
		when(sessionService.getOrCreate(USER_ID)).thenReturn(session);

		new OnboardingService(maxBotClient, sessionService, messageSource(), "test_bot", mock(ApplicantSessionService.class)).start(USER_ID);

		assertThat(session.getState()).isEqualTo(SessionState.WAITING_FOR_EGE_SCORE);
		assertThat(session.getEgeScores()).containsEntry("russian", 91);
		verifyNoInteractions(maxBotClient);
		verify(sessionService, never()).save(session);
	}

	@Test
	void firstBotStartBeginsOnboarding() {
		Session session = new Session();
		session.setUserId(USER_ID);
		SessionService sessionService = mock(SessionService.class);
		MaxBotClient maxBotClient = mock(MaxBotClient.class);
		when(sessionService.getOrCreate(USER_ID)).thenReturn(session);

		new OnboardingService(maxBotClient, sessionService, messageSource(), "test_bot", mock(ApplicantSessionService.class)).start(USER_ID);

		assertThat(session.getState()).isEqualTo(SessionState.WAITING_FOR_LANGUAGE);
		verify(sessionService).save(session);
		verify(maxBotClient).sendMessage(eq(USER_ID), any(NewMessageBody.class));
	}

	@Test
	void russiaIsFirstCitizenshipOptionAndLeadsToEge() {
		Session session = new Session();
		session.setUserId(USER_ID);
		session.setState(SessionState.WAITING_FOR_LANGUAGE);
		SessionService sessionService = mock(SessionService.class);
		MaxBotClient client = mock(MaxBotClient.class);
		when(sessionService.getOrCreate(USER_ID)).thenReturn(session);
		org.springframework.context.support.ResourceBundleMessageSource messages =
				new org.springframework.context.support.ResourceBundleMessageSource();
		messages.setBasename("messages");
		messages.setDefaultEncoding("UTF-8");
		messages.setFallbackToSystemLocale(false);
		OnboardingService service = new OnboardingService(client, sessionService, messages, "test_bot", mock(ApplicantSessionService.class));
		service.handleCallback(USER_ID, "language-callback", "lang_ru");
		org.mockito.ArgumentCaptor<NewMessageBody> question =
				org.mockito.ArgumentCaptor.forClass(NewMessageBody.class);
		verify(client).sendMessage(eq(USER_ID), question.capture());
		NewMessageBody.KeyboardPayload keyboard =
				(NewMessageBody.KeyboardPayload) question.getValue().getAttachments().getFirst().payload();
		assertThat(keyboard.buttons().getFirst().getFirst().text()).isEqualTo("Россия");
		assertThat(keyboard.buttons().getFirst().getFirst().payload()).isEqualTo("citizenship_RU");
		org.mockito.Mockito.clearInvocations(client);

		service.handleCallback(USER_ID, "country-callback", "citizenship_RU");

		assertThat(session.getCitizenship()).isEqualTo("RU");
		assertThat(session.getTrack()).isEqualTo("domestic_equivalent");
		assertThat(session.getState()).isEqualTo(SessionState.WAITING_FOR_EGE_SUBJECT);
		org.mockito.ArgumentCaptor<NewMessageBody> replies =
				org.mockito.ArgumentCaptor.forClass(NewMessageBody.class);
		verify(client, org.mockito.Mockito.times(2)).sendMessage(eq(USER_ID), replies.capture());
		assertThat(replies.getAllValues().getFirst().getText())
				.isEqualTo(messages.getMessage("track.russia", null, Locale.forLanguageTag("ru")));
		assertThat(replies.getAllValues().getLast().getText())
				.isEqualTo(messages.getMessage("ege.ask.subject", null, Locale.forLanguageTag("ru")));
	}

	@Test
	void restartCommandDropsSessionAndStartsOnboardingFromLanguage() {
		Session existing = sessionWaitingForScore();
		existing.getEgeScores().put("russian", 91);
		existing.setState(SessionState.WAITING_FOR_EGE_MORE);
		Session fresh = new Session();
		fresh.setUserId(USER_ID);
		SessionService sessionService = mock(SessionService.class);
		MaxBotClient maxBotClient = mock(MaxBotClient.class);
		when(sessionService.getOrCreate(USER_ID)).thenReturn(fresh);

		new OnboardingService(maxBotClient, sessionService, messageSource(), "test_bot", mock(ApplicantSessionService.class))
				.handleText(USER_ID, "/restart");

		verify(sessionService).delete(USER_ID);
		assertThat(fresh.getState()).isEqualTo(SessionState.WAITING_FOR_LANGUAGE);
		assertThat(fresh.getEgeScores()).isEmpty();
		verify(sessionService).save(fresh);
		verify(maxBotClient).sendMessage(eq(USER_ID), any(NewMessageBody.class));
	}

	@Test
	void restartCommandWithBotSuffixIsAlsoRecognized() {
		Session fresh = new Session();
		fresh.setUserId(USER_ID);
		SessionService sessionService = mock(SessionService.class);
		MaxBotClient maxBotClient = mock(MaxBotClient.class);
		when(sessionService.getOrCreate(USER_ID)).thenReturn(fresh);

		new OnboardingService(maxBotClient, sessionService, messageSource(), "test_bot", mock(ApplicantSessionService.class))
				.handleText(USER_ID, "/restart@t722_hakaton_max_bot");

		verify(sessionService).delete(USER_ID);
		assertThat(fresh.getState()).isEqualTo(SessionState.WAITING_FOR_LANGUAGE);
	}

	private static Session sessionWaitingForScore() {
		Session session = new Session();
		session.setUserId(USER_ID);
		session.setLanguage("ru");
		session.setCurrentSubject("math-profile");
		session.setState(SessionState.WAITING_FOR_EGE_SCORE);
		return session;
	}

	private static String lastMessageText(MaxBotClient maxBotClient) {
		org.mockito.ArgumentCaptor<NewMessageBody> sent =
				org.mockito.ArgumentCaptor.forClass(NewMessageBody.class);
		verify(maxBotClient, org.mockito.Mockito.atLeastOnce()).sendMessage(eq(USER_ID), sent.capture());
		return sent.getValue().getText();
	}

	private static StaticMessageSource messageSource() {
		StaticMessageSource messageSource = new StaticMessageSource();
		messageSource.addMessage("ege.more", Locale.forLanguageTag("ru"), "more");
		messageSource.addMessage("ege.score.invalid", Locale.forLanguageTag("ru"), "invalid");
		messageSource.addMessage("button.add", Locale.forLanguageTag("ru"), "add");
		messageSource.addMessage("button.done", Locale.forLanguageTag("ru"), "done");
		messageSource.addMessage("greeting", Locale.forLanguageTag("ru"), "greeting");
		messageSource.addMessage("language.ru", Locale.forLanguageTag("ru"), "Russian");
		messageSource.addMessage("language.kk", Locale.forLanguageTag("kk"), "Kazakh");
		messageSource.addMessage("language.ky", Locale.forLanguageTag("ky"), "Kyrgyz");
		return messageSource;
	}
}
