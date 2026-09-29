package com.runiversityadmisson.bot.application.olympiad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.runiversityadmisson.bot.application.dto.olympiad.OlympiadDiplomaInput;
import com.runiversityadmisson.bot.application.dto.olympiad.OlympiadDiplomaResponse;
import com.runiversityadmisson.bot.application.dto.olympiad.OlympiadProfileResponse;
import com.runiversityadmisson.bot.application.dto.olympiad.OlympiadResponse;
import com.runiversityadmisson.bot.domain.applicant.model.olympiad.Olympiad;
import com.runiversityadmisson.bot.domain.applicant.model.olympiad.OlympiadDegree;
import com.runiversityadmisson.bot.domain.applicant.model.olympiad.OlympiadDiploma;
import com.runiversityadmisson.bot.domain.applicant.model.olympiad.OlympiadProfile;
import com.runiversityadmisson.bot.domain.applicant.model.profile.User;
import com.runiversityadmisson.bot.domain.applicant.ports.olympiad.OlympiadProfileRepository;
import com.runiversityadmisson.bot.domain.applicant.ports.olympiad.OlympiadRepository;
import com.runiversityadmisson.bot.domain.applicant.ports.profile.UserRepository;
import com.runiversityadmisson.bot.presentation.exception.BadRequestException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OlympiadServiceTest {

	private final Olympiad vsosh = olympiad("vsosh", "Всероссийская олимпиада школьников", true);
	private final Olympiad pvg = olympiad("pvg", "Олимпиада школьников «Покори Воробьёвы горы!»", false);
	private final OlympiadProfile vsoshMath = profile(vsosh, "vsosh-math-2026", "math", "math-profile", null);
	private final OlympiadProfile pvgMath = profile(pvg, "pvg-math-2026", "math", "math-profile", 1);
	private final OlympiadProfile pvgPhysics = profile(pvg, "pvg-physics-2026", "physics", "physics", 1);

	private User user;
	private OlympiadService service;

	@BeforeEach
	void setUp() {
		user = new User();
		user.setId(UUID.randomUUID());
		UserRepository userRepository = mock(UserRepository.class);
		when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
		OlympiadRepository olympiadRepository = mock(OlympiadRepository.class);
		when(olympiadRepository.findAll()).thenReturn(List.of(pvg, vsosh));
		OlympiadProfileRepository profileRepository = mock(OlympiadProfileRepository.class);
		List<OlympiadProfile> allProfiles = List.of(vsoshMath, pvgMath, pvgPhysics);
		when(profileRepository.findAllById(anyIterable())).thenAnswer(invocation -> {
			Iterable<String> ids = invocation.getArgument(0);
			List<String> requested = StreamSupport.stream(ids.spliterator(), false).toList();
			return allProfiles.stream().filter(profile -> requested.contains(profile.getId())).toList();
		});
		service = new OlympiadService(olympiadRepository, profileRepository, userRepository);
	}

	@Test
	void listsVsoshFirst() {
		assertThat(service.getOlympiads(null, null)).extracting(OlympiadResponse::id).containsExactly("vsosh", "pvg");
	}

	@Test
	void searchIgnoresCaseAndYo() {
		List<OlympiadResponse> result = service.getOlympiads("  ВОРОБЬЕВЫ ", null);

		assertThat(result).extracting(OlympiadResponse::id).containsExactly("pvg");
	}

	@Test
	void filtersProfilesBySubject() {
		List<OlympiadResponse> result = service.getOlympiads(null, "physics");

		assertThat(result).extracting(OlympiadResponse::id).containsExactly("pvg");
		assertThat(result.getFirst().profiles()).extracting(OlympiadProfileResponse::id).containsExactly("pvg-physics-2026");
	}

	@Test
	void replacesDiplomas() {
		service.setDiplomas(user.getId(), List.of(new OlympiadDiplomaInput("pvg-physics-2026", "prize")));

		List<OlympiadDiplomaResponse> result = service.setDiplomas(user.getId(), List.of(
				new OlympiadDiplomaInput("pvg-math-2026", "winner"),
				new OlympiadDiplomaInput("vsosh-math-2026", "prize")));

		assertThat(user.getOlympiadDiplomas()).containsExactly(
				new OlympiadDiploma("pvg-math-2026", OlympiadDegree.WINNER),
				new OlympiadDiploma("vsosh-math-2026", OlympiadDegree.PRIZE));
		assertThat(result.getFirst()).isEqualTo(new OlympiadDiplomaResponse("pvg-math-2026", "pvg",
				"Олимпиада школьников «Покори Воробьёвы горы!»", "math", 1, "winner", 2026));
	}

	@Test
	void rejectsDuplicateDiplomas() {
		assertThatThrownBy(() -> service.setDiplomas(user.getId(), List.of(
				new OlympiadDiplomaInput("pvg-math-2026", "winner"),
				new OlympiadDiplomaInput("pvg-math-2026", "prize"))))
				.isInstanceOf(BadRequestException.class)
				.hasMessage("Дипломы не должны повторяться");
	}

	@Test
	void rejectsUnknownProfile() {
		assertThatThrownBy(() -> service.setDiplomas(user.getId(), List.of(new OlympiadDiplomaInput("unknown", "winner"))))
				.isInstanceOf(BadRequestException.class)
				.hasMessage("Указан неизвестный профиль олимпиады");
	}

	private static Olympiad olympiad(String id, String name, boolean vsosh) {
		Olympiad olympiad = new Olympiad();
		olympiad.setId(id);
		olympiad.setName(name);
		olympiad.setVsosh(vsosh);
		return olympiad;
	}

	private static OlympiadProfile profile(Olympiad olympiad, String id, String profileCode, String subjectId,
			Integer level) {
		OlympiadProfile profile = new OlympiadProfile();
		profile.setId(id);
		profile.setOlympiad(olympiad);
		profile.setProfile(profileCode);
		profile.setName(profileCode);
		profile.setSubjectId(subjectId);
		profile.setLevel(level);
		profile.setOlympiadYear(2026);
		olympiad.getProfiles().add(profile);
		return profile;
	}
}
