package com.runiversityadmisson.bot.application.onboarding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.runiversityadmisson.bot.application.dto.exam.EgeScoreInput;
import com.runiversityadmisson.bot.application.dto.profile.CitizenshipResultResponse;
import com.runiversityadmisson.bot.application.dto.exam.SubjectResponse;
import com.runiversityadmisson.bot.domain.applicant.model.profile.CitizenshipOption;
import com.runiversityadmisson.bot.domain.applicant.model.direction.InterestCategory;
import com.runiversityadmisson.bot.domain.applicant.model.exam.Subject;
import com.runiversityadmisson.bot.domain.applicant.model.profile.User;
import com.runiversityadmisson.bot.domain.applicant.ports.profile.CitizenshipOptionRepository;
import com.runiversityadmisson.bot.domain.applicant.ports.exam.EgeScoreRepository;
import com.runiversityadmisson.bot.domain.applicant.ports.direction.InterestCategoryRepository;
import com.runiversityadmisson.bot.domain.applicant.ports.profile.LanguageRepository;
import com.runiversityadmisson.bot.domain.applicant.ports.exam.SubjectRepository;
import com.runiversityadmisson.bot.domain.applicant.ports.profile.UserRepository;
import com.runiversityadmisson.bot.presentation.exception.BadRequestException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class QuestionnaireServiceTest {

	@Test
	void returnsSubjectsInUserLanguage() {
		User user = user("kk");
		Subject subject = new Subject();
		subject.setId("physics");
		subject.setNameRu("Физика");
		subject.setNameKk("Физика қазақша");
		subject.setMinThreshold(36);
		UserRepository userRepository = mock(UserRepository.class);
		SubjectRepository subjectRepository = mock(SubjectRepository.class);
		when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
		when(subjectRepository.findAll()).thenReturn(List.of(subject));

		List<SubjectResponse> response = service(userRepository, subjectRepository, mock(), mock())
				.getSubjects(user.getId());

		assertThat(response).containsExactly(new SubjectResponse("physics", "Физика қазақша", 36));
	}

	@Test
	void assignsQuotaTrackForOtherCitizenship() {
		User user = user("ru");
		CitizenshipOption option = new CitizenshipOption();
		option.setCode("OTHER");
		option.setGroupName("other");
		UserRepository userRepository = mock(UserRepository.class);
		CitizenshipOptionRepository citizenshipRepository = mock(CitizenshipOptionRepository.class);
		when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
		when(citizenshipRepository.findById("OTHER")).thenReturn(Optional.of(option));

		CitizenshipResultResponse response = service(userRepository, mock(), citizenshipRepository, mock())
				.setCitizenship(user.getId(), "OTHER");

		assertThat(response.track()).isEqualTo("rf_quota_or_paid");
		assertThat(user.getCitizenship()).isEqualTo("OTHER");
		assertThat(user.getTrack()).isEqualTo("rf_quota_or_paid");
	}

	@Test
	void rejectsDuplicateEgeSubjects() {
		User user = user("ru");
		UserRepository userRepository = mock(UserRepository.class);
		when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

		assertThatThrownBy(() -> service(userRepository, mock(), mock(), mock()).setEgeScores(user.getId(), List.of(
				new EgeScoreInput("physics", 70),
				new EgeScoreInput("physics", 80))))
				.isInstanceOf(BadRequestException.class)
				.hasMessage("Предметы ЕГЭ не должны повторяться");
	}

	@Test
	void replacesSelectedInterests() {
		User user = user("ru");
		InterestCategory it = new InterestCategory();
		it.setId("it");
		InterestCategory biology = new InterestCategory();
		biology.setId("biology");
		UserRepository userRepository = mock(UserRepository.class);
		InterestCategoryRepository interestRepository = mock(InterestCategoryRepository.class);
		when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
		when(interestRepository.findAllById(List.of("it", "biology"))).thenReturn(List.of(it, biology));

		service(userRepository, mock(), mock(), interestRepository).setInterests(user.getId(), List.of("it", "biology"));

		assertThat(user.getInterests()).extracting(InterestCategory::getId).containsExactlyInAnyOrder("it", "biology");
	}

	@Test
	void rejectsBelowMinimumBeforeDeletingStoredScores() {
		User user = user("ru");
		UserRepository users = mock(UserRepository.class);
		SubjectRepository subjects = mock(SubjectRepository.class);
		EgeScoreRepository scores = mock(EgeScoreRepository.class);
		when(users.findById(user.getId())).thenReturn(Optional.of(user));
		Subject physics = new Subject();
		physics.setId("physics");
		physics.setNameRu("Физика");
		physics.setMinThreshold(36);
		when(subjects.findAllById(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(physics));
		var service = new QuestionnaireService(users, mock(), mock(), subjects, scores, mock());
		assertThatThrownBy(() -> service.setEgeScores(user.getId(), List.of(new EgeScoreInput("physics", 35))))
				.isInstanceOf(BadRequestException.class).hasMessageContaining("от 36 до 100");
		org.mockito.Mockito.verifyNoInteractions(scores);
	}

	private QuestionnaireService service(
			UserRepository userRepository,
			SubjectRepository subjectRepository,
			CitizenshipOptionRepository citizenshipOptionRepository,
			InterestCategoryRepository interestCategoryRepository) {
		return new QuestionnaireService(
				userRepository,
				mock(LanguageRepository.class),
				citizenshipOptionRepository,
				subjectRepository,
				mock(EgeScoreRepository.class),
				interestCategoryRepository);
	}

	private User user(String language) {
		User user = new User();
		user.setId(UUID.randomUUID());
		user.setLanguage(language);
		return user;
	}
}
