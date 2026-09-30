package com.runiversityadmisson.bot.application.max.onboarding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.runiversityadmisson.bot.domain.applicant.model.exam.Subject;
import com.runiversityadmisson.bot.domain.applicant.ports.exam.SubjectRepository;
import com.runiversityadmisson.bot.infrastructure.external.max.MaxBotClient;
import com.runiversityadmisson.bot.infrastructure.external.max.dto.NewMessageBody;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.support.StaticMessageSource;

class OnboardingMessengerTest {
	@Test
	void keyboardUsesEntireCatalogIncludingPreviouslyMissingSubjects() {
		var subjects = List.of("russian", "math-profile", "physics", "informatics", "chemistry", "biology",
				"social-studies", "history", "geography", "literature", "foreign-language").stream().map(id -> {
			Subject subject = new Subject();
			subject.setId(id);
			subject.setNameRu(id);
			return subject;
		}).toList();
		SubjectRepository repository = mock(SubjectRepository.class);
		when(repository.findAll()).thenReturn(subjects);
		MaxBotClient client = mock(MaxBotClient.class);
		StaticMessageSource messages = new StaticMessageSource();
		messages.addMessage("ege.ask.subject", java.util.Locale.forLanguageTag("ru"), "Предмет");
		new OnboardingMessenger(client, messages, "app", repository).sendSubjectQuestion(1L, "ru");
		ArgumentCaptor<NewMessageBody> message = ArgumentCaptor.forClass(NewMessageBody.class);
		verify(client).sendMessage(eq(1L), message.capture());
		var keyboard = (NewMessageBody.KeyboardPayload) message.getValue().getAttachments().getFirst().payload();
		assertThat(keyboard.buttons()).allSatisfy(row -> assertThat(row).hasSizeBetween(1, 2));
		assertThat(keyboard.buttons().stream().flatMap(List::stream).map(NewMessageBody.Button::payload))
				.containsExactlyInAnyOrderElementsOf(subjects.stream().map(subject -> "subject_" + subject.getId()).toList());
	}
}
