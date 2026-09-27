package com.runiversityadmisson.bot.bot.service;

import com.runiversityadmisson.bot.bot.client.MaxBotClient;
import com.runiversityadmisson.bot.bot.client.dto.NewMessageBody;
import com.runiversityadmisson.bot.bot.session.Session;
import com.runiversityadmisson.bot.bot.session.SessionService;
import com.runiversityadmisson.bot.bot.session.SessionState;
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

		new OnboardingService(maxBotClient, sessionService, messageSource, "test_bot").handleText(USER_ID, "88");

		assertThat(session.getEgeScores()).containsEntry("math-profile", 88);
		assertThat(session.getState()).isEqualTo(SessionState.WAITING_FOR_EGE_MORE);
		verify(sessionService).save(session);
		verify(maxBotClient).sendMessage(eq(USER_ID), any(NewMessageBody.class));
	}

	@Test
	void scoreAboveRangeDoesNotChangeSessionAndSendsValidationMessage() {
		Session session = sessionWaitingForScore();
		SessionService sessionService = mock(SessionService.class);
		MaxBotClient maxBotClient = mock(MaxBotClient.class);
		StaticMessageSource messageSource = messageSource();
		when(sessionService.getOrCreate(USER_ID)).thenReturn(session);

		new OnboardingService(maxBotClient, sessionService, messageSource, "test_bot").handleText(USER_ID, "101");

		assertThat(session.getEgeScores()).isEmpty();
		assertThat(session.getState()).isEqualTo(SessionState.WAITING_FOR_EGE_SCORE);
		verify(maxBotClient).sendMessage(eq(USER_ID), any(NewMessageBody.class));
		verify(sessionService, never()).save(session);
	}

	@Test
	void repeatedBotStartPreservesExistingOnboardingProgress() {
		Session session = sessionWaitingForScore();
		session.getEgeScores().put("russian", 91);
		SessionService sessionService = mock(SessionService.class);
		MaxBotClient maxBotClient = mock(MaxBotClient.class);
		when(sessionService.getOrCreate(USER_ID)).thenReturn(session);

		new OnboardingService(maxBotClient, sessionService, messageSource(), "test_bot").start(USER_ID);

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

		new OnboardingService(maxBotClient, sessionService, messageSource(), "test_bot").start(USER_ID);

		assertThat(session.getState()).isEqualTo(SessionState.WAITING_FOR_LANGUAGE);
		verify(sessionService).save(session);
		verify(maxBotClient).sendMessage(eq(USER_ID), any(NewMessageBody.class));
	}

	private static Session sessionWaitingForScore() {
		Session session = new Session();
		session.setUserId(USER_ID);
		session.setLanguage("ru");
		session.setCurrentSubject("math-profile");
		session.setState(SessionState.WAITING_FOR_EGE_SCORE);
		return session;
	}

	private static StaticMessageSource messageSource() {
		StaticMessageSource messageSource = new StaticMessageSource();
		messageSource.addMessage("ege.more", Locale.forLanguageTag("ru"), "more");
		messageSource.addMessage("ege.invalid.score", Locale.forLanguageTag("ru"), "invalid");
		messageSource.addMessage("button.add", Locale.forLanguageTag("ru"), "add");
		messageSource.addMessage("button.done", Locale.forLanguageTag("ru"), "done");
		messageSource.addMessage("greeting", Locale.forLanguageTag("ru"), "greeting");
		messageSource.addMessage("language.ru", Locale.forLanguageTag("ru"), "Russian");
		messageSource.addMessage("language.kk", Locale.forLanguageTag("kk"), "Kazakh");
		messageSource.addMessage("language.ky", Locale.forLanguageTag("ky"), "Kyrgyz");
		return messageSource;
	}
}
