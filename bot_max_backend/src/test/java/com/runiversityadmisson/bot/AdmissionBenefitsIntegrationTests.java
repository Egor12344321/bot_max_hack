package com.runiversityadmisson.bot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.runiversityadmisson.bot.application.dto.exam.EgeScoreInput;
import com.runiversityadmisson.bot.application.dto.olympiad.OlympiadDiplomaInput;
import com.runiversityadmisson.bot.application.dto.benefit.AchievementScoreResponse;
import com.runiversityadmisson.bot.application.dto.benefit.AdmissionBenefitsResponse;
import com.runiversityadmisson.bot.application.dto.olympiad.OlympiadProfileResponse;
import com.runiversityadmisson.bot.application.dto.benefit.ProgramBenefitsResponse;
import com.runiversityadmisson.bot.application.dto.benefit.UniversityBenefitsResponse;
import com.runiversityadmisson.bot.application.benefit.AchievementPrivilegeService;
import com.runiversityadmisson.bot.application.benefit.AdmissionBenefitService;
import com.runiversityadmisson.bot.application.olympiad.OlympiadService;
import com.runiversityadmisson.bot.application.onboarding.OnboardingApiService;
import com.runiversityadmisson.bot.domain.applicant.model.profile.User;
import com.runiversityadmisson.bot.domain.applicant.ports.profile.UserRepository;
import com.runiversityadmisson.bot.presentation.exception.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

/** Миграции V9–V11 на настоящем Postgres и расчёт льгот по демо-правилам. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class AdmissionBenefitsIntegrationTests {

	@Autowired
	private UserRepository userRepository;
	@Autowired
	private OnboardingApiService onboardingApiService;
	@Autowired
	private OlympiadService olympiadService;
	@Autowired
	private AchievementPrivilegeService achievementPrivilegeService;
	@Autowired
	private AdmissionBenefitService admissionBenefitService;

	private UUID userId;

	@BeforeEach
	void createUser() {
		User user = new User();
		user.setMaxUserId(System.nanoTime());
		user.setLanguage("ru");
		userId = userRepository.save(user).getId();
	}

	@Test
	void seedsOlympiadCatalog() {
		assertThat(olympiadService.getOlympiads(null, null)).hasSize(21);
		assertThat(olympiadService.getOlympiads("физтех", null).getFirst().profiles())
				.extracting(OlympiadProfileResponse::profile, OlympiadProfileResponse::level)
				.containsExactly(
						tuple("informatics", 3),
						tuple("math", 2),
						tuple("physics", 1));
		assertThat(achievementPrivilegeService.getAchievements()).hasSize(5);
	}

	@Test
	void sameDiplomaGivesDifferentBenefitsInDifferentUniversities() {
		onboardingApiService.setEgeScores(userId, List.of(
				new EgeScoreInput("math-profile", 80),
				new EgeScoreInput("informatics", 90),
				new EgeScoreInput("russian", 88)));
		olympiadService.setDiplomas(userId, List.of(new OlympiadDiplomaInput("vysshaya-proba-math-2026", "prize")));
		achievementPrivilegeService.setAchievements(userId, List.of("medal_gold", "gto_gold"));

		AdmissionBenefitsResponse response = admissionBenefitService.getBenefits(userId, null);

		assertThat(response.universities()).extracting(UniversityBenefitsResponse::universityId)
				.containsExactly("hse", "itmo", "mipt", "msu");
		// ИТМО даёт БВИ за математику I уровня и призёрам.
		assertThat(programs(response, "itmo")).extracting(ProgramBenefitsResponse::benefit).containsOnly("bvi");
		assertThat(response.bviNote()).isNotNull();

		// ВШЭ: БВИ только победителям, призёру 100 баллов; ИД: медаль 3 + ГТО 2.
		ProgramBenefitsResponse hse = program(response, "hse", "hse-software");
		assertThat(hse.benefit()).isEqualTo("score_100");
		assertThat(hse.egeScoreWithBenefits()).isEqualTo(100 + 90 + 88);
		assertThat(hse.achievementPoints()).isEqualTo(5);
		assertThat(hse.totalScore()).isEqualTo(283);

		// МФТИ: для БВИ нужно 85 по математике, хватает только на 100 баллов; ИД из справочника не учитываются.
		ProgramBenefitsResponse mipt = program(response, "mipt", "mipt-applied-math");
		assertThat(mipt.benefit()).isEqualTo("score_100");
		assertThat(mipt.olympiads().getFirst().note()).contains("«БВИ»", "не ниже 85, сейчас 80");
		assertThat(mipt.achievements()).extracting(AchievementScoreResponse::counted).containsOnly(false);

		// МГУ: медаль 6 баллов, ГТО не учитывается.
		ProgramBenefitsResponse msu = program(response, "msu", "msu-math");
		assertThat(msu.benefit()).isEqualTo("score_100");
		assertThat(msu.totalScore()).isEqualTo(100 + 90 + 88 + 6);

		assertThat(admissionBenefitService.countBviPrograms(userId)).isEqualTo(3);
		assertThat(achievementPrivilegeService.setPrivileges(userId, List.of()).bestQuotaType()).isEqualTo("bvi");
	}

	@Test
	void filtersByUniversity() {
		AdmissionBenefitsResponse response = admissionBenefitService.getBenefits(userId, "msu");

		assertThat(response.universities()).extracting(UniversityBenefitsResponse::universityId).containsExactly("msu");
		assertThat(programs(response, "msu")).extracting(ProgramBenefitsResponse::missingSubjects)
				.allSatisfy(missing -> assertThat(missing).hasSize(3));
		assertThatThrownBy(() -> admissionBenefitService.getBenefits(userId, "unknown"))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	private static List<ProgramBenefitsResponse> programs(AdmissionBenefitsResponse response, String universityId) {
		return response.universities().stream()
				.filter(university -> university.universityId().equals(universityId))
				.findFirst()
				.orElseThrow()
				.programs();
	}

	private static ProgramBenefitsResponse program(AdmissionBenefitsResponse response, String universityId,
			String programId) {
		return programs(response, universityId).stream()
				.filter(program -> program.programId().equals(programId))
				.findFirst()
				.orElseThrow();
	}
}
