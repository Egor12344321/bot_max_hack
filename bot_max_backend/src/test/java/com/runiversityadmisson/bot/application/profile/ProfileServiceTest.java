package com.runiversityadmisson.bot.application.profile;

import static org.assertj.core.api.Assertions.assertThat;

import com.runiversityadmisson.bot.application.dto.planning.ProgramOptionResponse;
import com.runiversityadmisson.bot.application.dto.profile.ProfileProgramsResponse;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProfileServiceTest {

	@Test
	void countsProgramsByComparison() {
		ProfileProgramsResponse summary = ProfileService.summarize(List.of(
				option("bvi"), option("above_previous"), option("above_previous"),
				option("near_previous"), option("below_previous"), option("insufficient_data")));

		assertThat(summary).isEqualTo(new ProfileProgramsResponse(6, 1, 2, 1, 1, 1));
	}

	@Test
	void adviceAsksForMissingData() {
		ProfileProgramsResponse empty = new ProfileProgramsResponse(0, 0, 0, 0, 0, 0);

		assertThat(ProfileService.advice(true, true, empty)).startsWith("Добавь баллы ЕГЭ");
		assertThat(ProfileService.advice(false, true, empty)).startsWith("Выбери направления");
		assertThat(ProfileService.advice(false, false, empty)).startsWith("По выбранным направлениям подходящих программ нет");
	}

	@Test
	void adviceRemindsThatBviIsSingleUse() {
		assertThat(ProfileService.advice(false, false, new ProfileProgramsResponse(5, 2, 3, 0, 0, 0)))
				.startsWith("Программ с доступным БВИ: 2.");
	}

	@Test
	void adviceWarnsWhenMostProgramsAreBelowPassingScore() {
		assertThat(ProfileService.advice(false, false, new ProfileProgramsResponse(5, 0, 1, 1, 3, 0)))
				.startsWith("Большинство программ ниже прошлогоднего проходного");
	}

	@Test
	void adviceSuggestsKeepingSafePrograms() {
		assertThat(ProfileService.advice(false, false, new ProfileProgramsResponse(5, 0, 2, 1, 2, 0)))
				.startsWith("Программ с запасом: 2.");
	}

	private static ProgramOptionResponse option(String comparison) {
		return new ProgramOptionResponse("p", "p", "u", "u", null, "Москва", null, 2026, "budget", "full_time",
				"general", "eligible", "bvi".equals(comparison), 250, 250, 2025, 0, comparison, "demo",
				List.of(), List.of());
	}
}
