package com.runiversityadmisson.bot.application.planning;

import static org.assertj.core.api.Assertions.assertThat;

import com.runiversityadmisson.bot.application.dto.planning.ProgramOptionResponse;
import com.runiversityadmisson.bot.domain.applicant.model.university.Program;
import com.runiversityadmisson.bot.domain.applicant.model.university.ProgramSubject;
import java.util.List;
import org.junit.jupiter.api.Test;

class ApplicationPlanServiceTest {

	@Test
	void deficitLimitsHowFarBelowPassingScoreProgramCanBe() {
		assertThat(ApplicationPlanService.eligibleForPlan(option("eligible", false, -15), 15)).isTrue();
		assertThat(ApplicationPlanService.eligibleForPlan(option("eligible", false, -16), 15)).isFalse();
		assertThat(ApplicationPlanService.eligibleForPlan(option("eligible", false, 40), 0)).isTrue();
		assertThat(ApplicationPlanService.eligibleForPlan(option("eligible", false, null), 0)).isTrue();
	}

	@Test
	void bviAdmitsRegardlessOfExamsAndDeficit() {
		assertThat(ApplicationPlanService.eligibleForPlan(option("incomplete", true, null), 0)).isTrue();
		assertThat(ApplicationPlanService.eligibleForPlan(option("incomplete", false, null), 20)).isFalse();
		assertThat(ApplicationPlanService.eligibleForPlan(option("ineligible", false, null), 20)).isFalse();
	}

	@Test
	void relativePassingScoreDividesByMaximumSum() {
		Program threeExams = program(270, new ProgramSubject("russian", null), new ProgramSubject("math-profile", null),
				new ProgramSubject("informatics", 1), new ProgramSubject("physics", 1));
		Program unknown = program(null, new ProgramSubject("russian", null));

		assertThat(ApplicationPlanService.relativePassingScore(threeExams)).isEqualTo(0.9);
		assertThat(ApplicationPlanService.relativePassingScore(unknown)).isNull();
	}

	private static Program program(Integer passing, ProgramSubject... subjects) {
		Program program = new Program();
		program.setPassingScorePreviousYear(passing);
		program.setSubjects(List.of(subjects));
		return program;
	}

	private static ProgramOptionResponse option(String eligibility, boolean bvi, Integer difference) {
		return new ProgramOptionResponse("p", "p", "u", "u", null, "Москва", null, 2026, "budget", "full_time",
				"general", eligibility, bvi, 250, 250, 2025, difference, "near_previous", "demo", List.of(), List.of());
	}
}
