package com.runiversityadmisson.bot.application.max.onboarding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.runiversityadmisson.bot.application.max.dialog.BotQuestionnaire;
import com.runiversityadmisson.bot.application.max.dialog.BotQuestionnaireStore;
import com.runiversityadmisson.bot.application.max.dialog.BotQuestionnaireStep;
import com.runiversityadmisson.bot.domain.applicant.service.UserService;
import com.runiversityadmisson.bot.infrastructure.external.max.MaxBotClient;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OnboardingConversationTest {

	private static final Long USER_ID = 4242L;

	@Test
	void validEgeScoreIsSavedAndAdvancesSession() {
		BotQuestionnaire session = sessionWaitingForScore();
		BotQuestionnaireStore sessionService = mock(BotQuestionnaireStore.class);
		OnboardingMessenger botMessageService = mock(OnboardingMessenger.class);
		when(sessionService.getOrCreate(USER_ID)).thenReturn(session);

		service(sessionService, botMessageService, mock(UserService.class)).handleText(USER_ID, "88");

		assertThat(session.getEgeScores()).containsEntry("math-profile", 88);
		assertThat(session.getState()).isEqualTo(BotQuestionnaireStep.WAITING_FOR_EGE_MORE);
		verify(sessionService).save(session);
		verify(botMessageService).sendMoreSubjectsQuestion(USER_ID, "ru");
	}

	@Test
	void invalidScoreDoesNotChangeSession() {
		BotQuestionnaire session = sessionWaitingForScore();
		BotQuestionnaireStore sessionService = mock(BotQuestionnaireStore.class);
		OnboardingMessenger botMessageService = mock(OnboardingMessenger.class);
		when(sessionService.getOrCreate(USER_ID)).thenReturn(session);

		service(sessionService, botMessageService, mock(UserService.class)).handleText(USER_ID, "101");

		assertThat(session.getEgeScores()).isEmpty();
		assertThat(session.getState()).isEqualTo(BotQuestionnaireStep.WAITING_FOR_EGE_SCORE);
		verify(sessionService, never()).save(session);
		verify(botMessageService).sendInvalidScore(USER_ID, "ru");
	}

	@Test
	void firstBotStartBeginsOnboarding() {
		BotQuestionnaire session = new BotQuestionnaire();
		session.setUserId(USER_ID);
		BotQuestionnaireStore sessionService = mock(BotQuestionnaireStore.class);
		OnboardingMessenger botMessageService = mock(OnboardingMessenger.class);
		when(sessionService.getOrCreate(USER_ID)).thenReturn(session);

		service(sessionService, botMessageService, mock(UserService.class)).start(USER_ID);

		assertThat(session.getState()).isEqualTo(BotQuestionnaireStep.WAITING_FOR_LANGUAGE);
		verify(sessionService).save(session);
		verify(botMessageService).sendLanguageQuestion(USER_ID);
	}

	@Test
	void withoutLanguageAndCitizenshipStepsBotStartsWithEge() {
		BotQuestionnaire session = new BotQuestionnaire();
		session.setUserId(USER_ID);
		BotQuestionnaireStore sessionService = mock(BotQuestionnaireStore.class);
		OnboardingMessenger botMessageService = mock(OnboardingMessenger.class);
		when(sessionService.getOrCreate(USER_ID)).thenReturn(session);

		new OnboardingConversation(mock(MaxBotClient.class), sessionService, botMessageService,
				mock(UserService.class), false).start(USER_ID);

		assertThat(session.getState()).isEqualTo(BotQuestionnaireStep.WAITING_FOR_EGE_SUBJECT);
		assertThat(session.getLanguage()).isEqualTo("ru");
		assertThat(session.getCitizenship()).isEqualTo("RU");
		assertThat(session.getTrack()).isEqualTo("domestic_equivalent");
		verify(botMessageService, never()).sendLanguageQuestion(USER_ID);
		verify(botMessageService).sendEgeGreeting(USER_ID, "ru");
		verify(botMessageService).sendSubjectQuestion(USER_ID, "ru");
	}

	@Test
	void russiaLeadsToEgeTrack() {
		BotQuestionnaire session = new BotQuestionnaire();
		session.setUserId(USER_ID);
		session.setState(BotQuestionnaireStep.WAITING_FOR_LANGUAGE);
		BotQuestionnaireStore sessionService = mock(BotQuestionnaireStore.class);
		MaxBotClient maxBotClient = mock(MaxBotClient.class);
		OnboardingMessenger botMessageService = mock(OnboardingMessenger.class);
		when(sessionService.getOrCreate(USER_ID)).thenReturn(session);
		OnboardingConversation service = new OnboardingConversation(maxBotClient, sessionService, botMessageService, mock(UserService.class));

		service.handleCallback(USER_ID, "language-callback", "lang_ru");
		service.handleCallback(USER_ID, "country-callback", "citizenship_RU");

		assertThat(session.getCitizenship()).isEqualTo("RU");
		assertThat(session.getTrack()).isEqualTo("domestic_equivalent");
		assertThat(session.getState()).isEqualTo(BotQuestionnaireStep.WAITING_FOR_EGE_SUBJECT);
		verify(maxBotClient).answerCallback("language-callback", "Принято");
		verify(maxBotClient).answerCallback("country-callback", "Принято");
		verify(botMessageService).sendCitizenshipQuestion(USER_ID, "ru");
		verify(botMessageService).sendTrackMessage(USER_ID, "ru", "track.russia");
		verify(botMessageService).sendSubjectQuestion(USER_ID, "ru");
	}

	@Test
	void foreignerCompletionPersistsUserAndClearsRedisSession() {
		BotQuestionnaire session = new BotQuestionnaire();
		session.setUserId(USER_ID);
		session.setLanguage("ru");
		session.setState(BotQuestionnaireStep.WAITING_FOR_CITIZENSHIP);
		BotQuestionnaireStore sessionService = mock(BotQuestionnaireStore.class);
		OnboardingMessenger botMessageService = mock(OnboardingMessenger.class);
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
		BotQuestionnaire fresh = new BotQuestionnaire();
		fresh.setUserId(USER_ID);
		BotQuestionnaireStore sessionService = mock(BotQuestionnaireStore.class);
		OnboardingMessenger botMessageService = mock(OnboardingMessenger.class);
		UserService userService = mock(UserService.class);
		when(sessionService.getOrCreate(USER_ID)).thenReturn(fresh);

		service(sessionService, botMessageService, userService).handleText(USER_ID, "/restart");

		verify(sessionService).delete(USER_ID);
		verify(userService).deleteByMaxUserId(USER_ID);
		assertThat(fresh.getState()).isEqualTo(BotQuestionnaireStep.WAITING_FOR_LANGUAGE);
		verify(botMessageService).sendLanguageQuestion(USER_ID);
	}

	private static OnboardingConversation service(
			BotQuestionnaireStore sessionService,
			OnboardingMessenger botMessageService,
			UserService userService) {
		return new OnboardingConversation(mock(MaxBotClient.class), sessionService, botMessageService, userService);
	}

	private static BotQuestionnaire sessionWaitingForScore() {
		BotQuestionnaire session = new BotQuestionnaire();
		session.setUserId(USER_ID);
		session.setLanguage("ru");
		session.setCurrentSubject("math-profile");
		session.setState(BotQuestionnaireStep.WAITING_FOR_EGE_SCORE);
		return session;
	}
}
