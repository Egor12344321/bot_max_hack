package com.runiversityadmisson.bot.application.max.onboarding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
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
import org.mockito.InOrder;
import java.util.Optional;
import com.runiversityadmisson.bot.domain.applicant.model.exam.Subject;
import com.runiversityadmisson.bot.domain.applicant.ports.exam.SubjectRepository;
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
				mock(UserService.class), false, subjects()).start(USER_ID);

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
		OnboardingConversation service = new OnboardingConversation(maxBotClient, sessionService, botMessageService, mock(UserService.class), subjects());

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

	@Test
	void botStartResetsUnfinishedQuestionnaireButKeepsSavedApplication() {
		BotQuestionnaire fresh = new BotQuestionnaire();
		fresh.setUserId(USER_ID);
		BotQuestionnaireStore sessionService = mock(BotQuestionnaireStore.class);
		OnboardingMessenger botMessageService = mock(OnboardingMessenger.class);
		UserService userService = mock(UserService.class);
		// После удаления из Redis store отдаёт новую анкету на шаге NEW.
		when(sessionService.getOrCreate(USER_ID)).thenReturn(fresh);

		service(sessionService, botMessageService, userService).restartFromBotStart(USER_ID);

		InOrder order = inOrder(sessionService);
		order.verify(sessionService).delete(USER_ID);
		order.verify(sessionService).getOrCreate(USER_ID);
		verify(userService, never()).deleteByMaxUserId(USER_ID);
		assertThat(fresh.getState()).isEqualTo(BotQuestionnaireStep.WAITING_FOR_LANGUAGE);
		verify(botMessageService).sendLanguageQuestion(USER_ID);
	}

	private static OnboardingConversation service(
			BotQuestionnaireStore sessionService,
			OnboardingMessenger botMessageService,
			UserService userService) {
		return new OnboardingConversation(mock(MaxBotClient.class), sessionService, botMessageService, userService, subjects());
	}

	private static SubjectRepository subjects() {
		SubjectRepository repository = mock(SubjectRepository.class);
		for (String id : java.util.List.of("math-profile", "russian", "informatics")) {
			Subject subject = new Subject();
			subject.setId(id);
			subject.setMinThreshold(40);
			when(repository.findById(id)).thenReturn(Optional.of(subject));
			when(repository.existsById(id)).thenReturn(true);
		}
		return repository;
	}

	@Test
	void belowMinimumDoesNotSaveOrAdvance() {
		BotQuestionnaire session = sessionWaitingForScore();
		BotQuestionnaireStore store = mock(BotQuestionnaireStore.class);
		OnboardingMessenger messenger = mock(OnboardingMessenger.class);
		when(store.getOrCreate(USER_ID)).thenReturn(session);
		service(store, messenger, mock(UserService.class)).handleText(USER_ID, "39");
		assertThat(session.getEgeScores()).isEmpty();
		assertThat(session.getState()).isEqualTo(BotQuestionnaireStep.WAITING_FOR_EGE_SCORE);
		verify(messenger).sendBelowMinimum(USER_ID, "ru", 40);
		verify(store, never()).save(session);
	}

	@Test
	void forgedFinishAndUnknownSubjectCannotBypassOnboarding() {
		BotQuestionnaire session = sessionWaitingForScore();
		session.setState(BotQuestionnaireStep.WAITING_FOR_EGE_SUBJECT);
		BotQuestionnaireStore store = mock(BotQuestionnaireStore.class);
		when(store.getOrCreate(USER_ID)).thenReturn(session);
		UserService users = mock(UserService.class);
		var conversation = service(store, mock(OnboardingMessenger.class), users);
		conversation.handleCallback(USER_ID, null, "ege_done");
		conversation.handleCallback(USER_ID, null, "subject_fake");
		assertThat(session.getState()).isEqualTo(BotQuestionnaireStep.WAITING_FOR_EGE_SUBJECT);
		org.mockito.Mockito.verifyNoInteractions(users);
	}

	@Test
	void finishRequiresCompleteScoresAndPersistsAtBoundary() {
		BotQuestionnaire session = sessionWaitingForScore();
		session.setCitizenship("RU");
		session.setTrack("domestic_equivalent");
		session.setState(BotQuestionnaireStep.WAITING_FOR_EGE_MORE);
		session.getEgeScores().put("math-profile", 40);
		BotQuestionnaireStore store = mock(BotQuestionnaireStore.class);
		when(store.getOrCreate(USER_ID)).thenReturn(session);
		UserService users = mock(UserService.class);
		var conversation = service(store, mock(OnboardingMessenger.class), users);
		conversation.handleCallback(USER_ID, null, "ege_done");
		org.mockito.Mockito.verifyNoInteractions(users);
		session.getEgeScores().put("russian", 40);
		session.getEgeScores().put("informatics", 40);
		conversation.handleCallback(USER_ID, null, "ege_done");
		verify(users).syncFromBot(USER_ID, "ru", "RU", "domestic_equivalent", session.getEgeScores());
		verify(store).delete(USER_ID);
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
