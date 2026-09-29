package com.runiversityadmisson.bot.application.onboarding.bot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.runiversityadmisson.bot.application.session.Session;
import com.runiversityadmisson.bot.application.session.SessionService;
import com.runiversityadmisson.bot.application.session.SessionState;
import com.runiversityadmisson.bot.domain.applicant.service.UserService;
import com.runiversityadmisson.bot.infrastructure.external.max.MaxBotClient;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OnboardingServiceTest {

	private static final Long USER_ID = 4242L;

	@Test
	void validEgeScoreIsSavedAndAdvancesSession() {
		Session session = sessionWaitingForScore();
		SessionService sessionService = mock(SessionService.class);
		BotMessageService botMessageService = mock(BotMessageService.class);
		when(sessionService.getOrCreate(USER_ID)).thenReturn(session);

		service(sessionService, botMessageService, mock(UserService.class)).handleText(USER_ID, "88");

		assertThat(session.getEgeScores()).containsEntry("math-profile", 88);
		assertThat(session.getState()).isEqualTo(SessionState.WAITING_FOR_EGE_MORE);
		verify(sessionService).save(session);
		verify(botMessageService).sendMoreSubjectsQuestion(USER_ID, "ru");
	}

	@Test
	void invalidScoreDoesNotChangeSession() {
		Session session = sessionWaitingForScore();
		SessionService sessionService = mock(SessionService.class);
		BotMessageService botMessageService = mock(BotMessageService.class);
		when(sessionService.getOrCreate(USER_ID)).thenReturn(session);

		service(sessionService, botMessageService, mock(UserService.class)).handleText(USER_ID, "101");

		assertThat(session.getEgeScores()).isEmpty();
		assertThat(session.getState()).isEqualTo(SessionState.WAITING_FOR_EGE_SCORE);
		verify(sessionService, never()).save(session);
		verify(botMessageService).sendInvalidScore(USER_ID, "ru");
	}

	@Test
	void firstBotStartBeginsOnboarding() {
		Session session = new Session();
		session.setUserId(USER_ID);
		SessionService sessionService = mock(SessionService.class);
		BotMessageService botMessageService = mock(BotMessageService.class);
		when(sessionService.getOrCreate(USER_ID)).thenReturn(session);

		service(sessionService, botMessageService, mock(UserService.class)).start(USER_ID);

		assertThat(session.getState()).isEqualTo(SessionState.WAITING_FOR_LANGUAGE);
		verify(sessionService).save(session);
		verify(botMessageService).sendLanguageQuestion(USER_ID);
	}

	@Test
	void russiaLeadsToEgeTrack() {
		Session session = new Session();
		session.setUserId(USER_ID);
		session.setState(SessionState.WAITING_FOR_LANGUAGE);
		SessionService sessionService = mock(SessionService.class);
		MaxBotClient maxBotClient = mock(MaxBotClient.class);
		BotMessageService botMessageService = mock(BotMessageService.class);
		when(sessionService.getOrCreate(USER_ID)).thenReturn(session);
		OnboardingService service = new OnboardingService(maxBotClient, sessionService, botMessageService, mock(UserService.class));

		service.handleCallback(USER_ID, "language-callback", "lang_ru");
		service.handleCallback(USER_ID, "country-callback", "citizenship_RU");

		assertThat(session.getCitizenship()).isEqualTo("RU");
		assertThat(session.getTrack()).isEqualTo("domestic_equivalent");
		assertThat(session.getState()).isEqualTo(SessionState.WAITING_FOR_EGE_SUBJECT);
		verify(maxBotClient).answerCallback("language-callback", "Принято");
		verify(maxBotClient).answerCallback("country-callback", "Принято");
		verify(botMessageService).sendCitizenshipQuestion(USER_ID, "ru");
		verify(botMessageService).sendTrackMessage(USER_ID, "ru", "track.russia");
		verify(botMessageService).sendSubjectQuestion(USER_ID, "ru");
	}

	@Test
	void foreignerCompletionPersistsUserAndClearsRedisSession() {
		Session session = new Session();
		session.setUserId(USER_ID);
		session.setLanguage("ru");
		session.setState(SessionState.WAITING_FOR_CITIZENSHIP);
		SessionService sessionService = mock(SessionService.class);
		BotMessageService botMessageService = mock(BotMessageService.class);
		UserService userService = mock(UserService.class);
		when(sessionService.getOrCreate(USER_ID)).thenReturn(session);

		service(sessionService, botMessageService, userService).handleCallback(USER_ID, null, "citizenship_OTHER");

		verify(userService).syncFromBot(eq(USER_ID), eq("ru"), eq("OTHER"), eq("rf_quota_or_paid"),
				org.mockito.ArgumentMatchers.<Map<String, Integer>>any());
		verify(sessionService).delete(USER_ID);
		verify(botMessageService).sendForeignerInfo(USER_ID, "ru");
	}

	@Test
	void restartClearsRedisAndPersistedSession() {
		Session fresh = new Session();
		fresh.setUserId(USER_ID);
		SessionService sessionService = mock(SessionService.class);
		BotMessageService botMessageService = mock(BotMessageService.class);
		UserService userService = mock(UserService.class);
		when(sessionService.getOrCreate(USER_ID)).thenReturn(fresh);

		service(sessionService, botMessageService, userService).handleText(USER_ID, "/restart");

		verify(sessionService).delete(USER_ID);
		verify(userService).deleteByMaxUserId(USER_ID);
		assertThat(fresh.getState()).isEqualTo(SessionState.WAITING_FOR_LANGUAGE);
		verify(botMessageService).sendLanguageQuestion(USER_ID);
	}

	private static OnboardingService service(
			SessionService sessionService,
			BotMessageService botMessageService,
			UserService userService) {
		return new OnboardingService(mock(MaxBotClient.class), sessionService, botMessageService, userService);
	}

	private static Session sessionWaitingForScore() {
		Session session = new Session();
		session.setUserId(USER_ID);
		session.setLanguage("ru");
		session.setCurrentSubject("math-profile");
		session.setState(SessionState.WAITING_FOR_EGE_SCORE);
		return session;
	}
}
