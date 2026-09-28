package com.runiversityadmisson.bot.web.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.runiversityadmisson.bot.web.dto.SessionDraftResponse;
import com.runiversityadmisson.bot.web.model.EgeScore;
import com.runiversityadmisson.bot.web.model.Subject;
import com.runiversityadmisson.bot.web.model.User;
import com.runiversityadmisson.bot.web.repository.EgeScoreRepository;
import com.runiversityadmisson.bot.web.repository.SubjectRepository;
import com.runiversityadmisson.bot.web.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SessionQueryServiceTest {

	@Test
	void mapsUserAndScoresToSessionDraft() {
		UserRepository userRepository = mock(UserRepository.class);
		EgeScoreRepository egeScoreRepository = mock(EgeScoreRepository.class);
		SubjectRepository subjectRepository = mock(SubjectRepository.class);
		UUID userId = UUID.randomUUID();
		User user = new User();
		user.setId(userId);
		user.setLanguage("kk");
		user.setCitizenship("KZ");
		EgeScore score = new EgeScore();
		score.setSubjectId("math-profile");
		score.setScore(80);
		Subject subject = new Subject();
		subject.setId("math-profile");
		subject.setNameRu("Математика");
		subject.setNameKk("Математика");
		subject.setMinThreshold(27);

		when(userRepository.findById(userId)).thenReturn(Optional.of(user));
		when(egeScoreRepository.findByUserId(userId)).thenReturn(List.of(score));
		when(subjectRepository.findAllById(org.mockito.ArgumentMatchers.<String>anyList())).thenReturn(List.of(subject));

		SessionDraftResponse response = new SessionQueryService(
				userRepository, egeScoreRepository, subjectRepository).getSession(userId);

		assertThat(response.id()).isEqualTo(userId);
		assertThat(response.language()).isEqualTo("kk");
		assertThat(response.countryCode()).isEqualTo("KZ");
		assertThat(response.isCompleteFromBot()).isTrue();
		assertThat(response.egeScores()).singleElement().satisfies(result -> {
			assertThat(result.subjectName()).isEqualTo("Математика");
			assertThat(result.minThreshold()).isEqualTo(27);
			assertThat(result.passed()).isTrue();
		});
	}
}
