package com.runiversityadmisson.bot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.runiversityadmisson.bot.application.benefit.AchievementPrivilegeService;
import com.runiversityadmisson.bot.application.direction.StudyDirectionService;
import com.runiversityadmisson.bot.application.dto.direction.StudyDirectionResponse;
import com.runiversityadmisson.bot.application.dto.exam.EgeScoreInput;
import com.runiversityadmisson.bot.application.dto.exam.EgeScoreResponse;
import com.runiversityadmisson.bot.application.dto.olympiad.OlympiadDiplomaInput;
import com.runiversityadmisson.bot.application.dto.planning.ProgramBreakdownItemResponse;
import com.runiversityadmisson.bot.application.dto.planning.ProgramOptionPageResponse;
import com.runiversityadmisson.bot.application.dto.planning.ProgramOptionResponse;
import com.runiversityadmisson.bot.application.dto.profile.ProfileProgramsResponse;
import com.runiversityadmisson.bot.application.dto.profile.ProfileResponse;
import com.runiversityadmisson.bot.application.olympiad.OlympiadService;
import com.runiversityadmisson.bot.application.onboarding.QuestionnaireService;
import com.runiversityadmisson.bot.application.planning.RecommendationService;
import com.runiversityadmisson.bot.application.profile.ProfileService;
import com.runiversityadmisson.bot.domain.applicant.model.profile.User;
import com.runiversityadmisson.bot.domain.applicant.ports.profile.UserRepository;
import com.runiversityadmisson.bot.presentation.exception.BadRequestException;
import com.runiversityadmisson.bot.presentation.exception.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

/** Рекомендации по направлению «Программная инженерия» на данных V13 (проходные 2025 года). */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class RecommendationsIntegrationTests {

	private static final String SOFTWARE_ENGINEERING = "09.03.04";

	@Autowired
	private UserRepository userRepository;
	@Autowired
	private QuestionnaireService questionnaireService;
	@Autowired
	private StudyDirectionService directionService;
	@Autowired
	private OlympiadService olympiadService;
	@Autowired
	private AchievementPrivilegeService achievementPrivilegeService;
	@Autowired
	private RecommendationService recommendationService;
	@Autowired
	private ProfileService profileService;
	@Autowired
	private com.runiversityadmisson.bot.application.planning.RecommendationSettingsService settings;
	@Autowired
	private com.runiversityadmisson.bot.domain.applicant.ports.university.ProgramRepository programs;

	private UUID userId;

	@BeforeEach
	void createUser() {
		User user = new User();
		user.setMaxUserId(System.nanoTime());
		user.setLanguage("ru");
		userId = userRepository.save(user).getId();
		directionService.replaceSelection(userId, List.of(SOFTWARE_ENGINEERING));
		questionnaireService.setEgeScores(userId, List.of(
				new EgeScoreInput("math-profile", 80),
				new EgeScoreInput("informatics", 85),
				new EgeScoreInput("russian", 90)));
		achievementPrivilegeService.setAchievements(userId, List.of("medal_gold"));
	}

	@Test
	void ranksByPassingScoreAndKeepsSafePrograms() {
		ProgramOptionPageResponse page = recommendationService.getRecommendations(userId, SOFTWARE_ENGINEERING, 0, 20);

		// 255 по ЕГЭ + 5 за медаль = 260. МГТУ (302), ВШЭ (297), МФТИ (299), МАИ (290), МТУСИ (285)
		// слишком высоко. РХТУ с большим запасом остаётся, МИФИ без проходного идёт в конец.
		assertThat(page.items()).extracting(ProgramOptionResponse::programId).containsExactly(
				"fa-090304", "miet-090304", "stankin-090304", "mirea-090304", "rgsu-090304", "muctr-090304", "mephi-090304");
		assertThat(page.total()).isEqualTo(7);
		assertThat(page.items()).extracting(ProgramOptionResponse::comparison).containsExactly(
				"below_previous", "below_previous", "near_previous", "above_previous", "above_previous", "above_previous",
				"insufficient_data");

		ProgramOptionResponse stankin = page.items().get(2);
		assertThat(stankin.totalScore()).isEqualTo(260);
		assertThat(stankin.passingScorePreviousYear()).isEqualTo(262);
		assertThat(stankin.previousYear()).isEqualTo(2025);
		assertThat(stankin.scoreDifference()).isEqualTo(-2);
		assertThat(stankin.eligibility()).isEqualTo("eligible");
		assertThat(stankin.direction().code()).isEqualTo(SOFTWARE_ENGINEERING);
		assertThat(stankin.city()).isEqualTo("Москва");
		assertThat(stankin.breakdown()).extracting(ProgramBreakdownItemResponse::kind)
				.containsExactly("exam", "exam", "exam", "achievement");

		ProgramOptionResponse mephi = page.items().getLast();
		assertThat(mephi.scoreDifference()).isNull();
		assertThat(mephi.reasons()).contains("В 2025 году все бюджетные места заняли абитуриенты с БВИ");
	}

	@Test
	void explainsChosenAlternativeSubject() {
		questionnaireService.setEgeScores(userId, List.of(
				new EgeScoreInput("math-profile", 80),
				new EgeScoreInput("informatics", 85),
				new EgeScoreInput("physics", 70),
				new EgeScoreInput("russian", 90)));

		ProgramOptionResponse fa = recommendationService.getRecommendations(userId, SOFTWARE_ENGINEERING, 0, 20)
				.items().stream().filter(option -> option.programId().equals("fa-090304")).findFirst().orElseThrow();

		ProgramBreakdownItemResponse physics = fa.breakdown().stream()
				.filter(item -> item.name().equals("Физика")).findFirst().orElseThrow();
		assertThat(physics.applied()).isFalse();
		assertThat(physics.explanation()).isEqualTo("Не засчитан: выбран предмет «Информатика» (85 ≥ 70)");
	}

	@Test
	void capsDisplayedBviProgramsButRetainsFullCandidateSet() {
		olympiadService.setDiplomas(userId,
				List.of(new OlympiadDiplomaInput("vysshaya-proba-informatics-2026", "winner")));

		ProgramOptionPageResponse page = recommendationService.getRecommendations(userId, SOFTWARE_ENGINEERING, 0, 20);

		// Победитель I уровня по информатике: БВИ во всех вузах, кроме МФТИ. Их 11 — больше обычного лимита 10.
		assertThat(page.total()).isEqualTo(10);
		assertThat(recommendationService.evaluatePrograms(userId,
				programs.findByDirectionIdOrderByUniversityIdAscIdAsc(SOFTWARE_ENGINEERING)).stream()
				.filter(ProgramOptionResponse::bviAvailable)).hasSize(11);
		assertThat(page.items()).allSatisfy(option -> {
			assertThat(option.bviAvailable()).isTrue();
			assertThat(option.comparison()).isEqualTo("bvi");
		});
		// В общем конкурсе информатика засчитывается как 100.
		ProgramOptionResponse stankin = page.items().stream()
				.filter(option -> option.programId().equals("stankin-090304")).findFirst().orElseThrow();
		assertThat(stankin.totalScore()).isEqualTo(80 + 100 + 90 + 5);
		assertThat(stankin.reasons()).contains("БВИ можно использовать только в одном вузе и только на одном направлении");
	}

	@Test
	void paginatesSelectedPrograms() {
		ProgramOptionPageResponse page = recommendationService.getRecommendations(userId, SOFTWARE_ENGINEERING, 4, 20);

		assertThat(page.items()).extracting(ProgramOptionResponse::programId).containsExactly("rgsu-090304", "muctr-090304", "mephi-090304");
		assertThat(page.total()).isEqualTo(7);
	}

	@Test
	void profileSummarizesRecommendations() {
		ProfileResponse profile = profileService.getProfile(userId);

		assertThat(profile.egeTotal()).isEqualTo(80 + 85 + 90);
		assertThat(profile.egeScores()).hasSize(3);
		assertThat(profile.directions()).extracting(StudyDirectionResponse::id).containsExactly(SOFTWARE_ENGINEERING);
		assertThat(profile.achievements()).containsExactly("Медаль «За особые успехи в учении» I степени");
		// Та же подборка, что в showsOnlyProgramsWithinWindowSortedByDifference.
		assertThat(profile.programs()).isEqualTo(new ProfileProgramsResponse(7, 0, 3, 1, 2, 1, 1, 2, 3));
		assertThat(profile.advice()).startsWith("Программ с запасом: 1.");
	}

	@Test
	void svoUsesDemoPassingScoreOfSeparateCompetition() {
		achievementPrivilegeService.setPrivileges(userId, List.of("svo_participant"));
		var page = recommendationService.getRecommendations(userId, SOFTWARE_ENGINEERING, 0, 100);
		assertThat(page.total()).isEqualTo(10);
		assertThat(page.items()).allSatisfy(option -> {
			assertThat(option.competitionType()).isEqualTo("separate_quota");
			assertThat(option.seats()).isNull();
			assertThat(option.availableCompetitionTypes()).contains("general", "separate_quota");
			Integer general = programs.findById(option.programId()).orElseThrow().getPassingScorePreviousYear();
			if (general == null) {
				assertThat(option.passingScorePreviousYear()).isNull();
				assertThat(option.comparison()).isEqualTo("insufficient_data");
			} else {
				// Демо-проходной квоты: 88% проходного общего конкурса, а не он сам.
				assertThat(option.passingScorePreviousYear()).isEqualTo(Math.round(general * 0.88f));
				assertThat(option.scoreDifference()).isEqualTo(option.totalScore() - option.passingScorePreviousYear());
				assertThat(option.dataSource()).isEqualTo("demo");
				assertThat(option.reasons()).anyMatch(reason -> reason.startsWith("Проходной по квоте демонстрационный"));
			}
		});
		assertThat(page.items()).anySatisfy(option -> {
			assertThat(option.programId()).isEqualTo("stankin-090304");
			assertThat(option.passingScorePreviousYear()).isEqualTo(231);
		});
		settings.save(userId, 15, "general");
		assertThat(recommendationService.getRecommendations(userId, SOFTWARE_ENGINEERING, 0, 100).items())
				.allSatisfy(option -> assertThat(option.competitionType()).isEqualTo("general"));
	}

	@Test
	void usesSeatsAndPassingScoreOfSelectedQuota() {
		achievementPrivilegeService.setPrivileges(userId, List.of("svo_child"));
		var quota = programs.findById("stankin-090304").orElseThrow().getCompetitions().get("separate_quota");
		quota.setSeats(7);
		quota.setPassingScore(240);
		quota.setPreviousYear(2025);
		var option = recommendationService.getRecommendations(userId, SOFTWARE_ENGINEERING, 0, 100).items().stream()
				.filter(item -> item.programId().equals("stankin-090304")).findFirst().orElseThrow();
		assertThat(option.seats()).isEqualTo(7);
		assertThat(option.scoreDifference()).isEqualTo(20);
		quota.setSeats(0);
		assertThat(recommendationService.getRecommendations(userId, SOFTWARE_ENGINEERING, 0, 100).items())
				.noneMatch(item -> item.programId().equals("stankin-090304"));
	}

	@Test
	void validatesMinimumBeforeReplacingExistingScores() {
		assertThatThrownBy(() -> questionnaireService.setEgeScores(userId,
				List.of(new EgeScoreInput("russian", 35))))
				.isInstanceOf(BadRequestException.class);
		assertThat(questionnaireService.getEgeScores(userId).scores()).hasSize(3);
	}

	@Test
	void profileWorksWithoutLanguage() {
		User user = userRepository.findById(userId).orElseThrow();
		user.setLanguage(null);

		assertThat(profileService.getProfile(userId).egeScores())
				.extracting(EgeScoreResponse::subjectName)
				.contains("Информатика");
	}

	@Test
	void rejectsUnselectedAndUnknownDirections() {
		assertThatThrownBy(() -> recommendationService.getRecommendations(userId, "01.03.02", 0, 20))
				.isInstanceOf(BadRequestException.class)
				.hasMessage("Направление не выбрано");
		assertThatThrownBy(() -> recommendationService.getRecommendations(userId, "99.99.99", 0, 20))
				.isInstanceOf(ResourceNotFoundException.class);
		assertThatThrownBy(() -> recommendationService.getRecommendations(userId, SOFTWARE_ENGINEERING, 0, 0))
				.isInstanceOf(BadRequestException.class);
	}
}
