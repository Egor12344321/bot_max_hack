package com.runiversityadmisson.bot.domain.applicant.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.runiversityadmisson.bot.domain.applicant.service.ApplicationPlanGenerator.BaseUniversity;
import com.runiversityadmisson.bot.domain.applicant.service.ApplicationPlanGenerator.Candidate;
import com.runiversityadmisson.bot.domain.applicant.service.ApplicationPlanGenerator.ProgramChoice;
import com.runiversityadmisson.bot.domain.applicant.service.ApplicationPlanGenerator.Reason;
import com.runiversityadmisson.bot.domain.applicant.service.ApplicationPlanGenerator.Result;
import com.runiversityadmisson.bot.domain.applicant.service.ApplicationPlanGenerator.TopPlace;
import com.runiversityadmisson.bot.domain.applicant.service.ApplicationPlanGenerator.UniversityChoice;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ApplicationPlanGeneratorTest {

	private static final String SE = "09.03.04";
	private static final String AMI = "01.03.02";
	private static final String IS = "10.03.01";
	private static final String LAW = "40.03.01";

	private final ApplicationPlanGenerator generator = new ApplicationPlanGenerator();

	@Test
	void takesTopThreeDistinctUniversitiesByPassingScore() {
		Result result = generate(List.of(SE),
				program("a-se", "a", SE, 0.95),
				program("a-se2", "a", SE, 0.94),
				program("b-se", "b", SE, 0.90),
				program("c-se", "c", SE, 0.85));

		assertThat(result.universities()).extracting(UniversityChoice::universityId).containsExactly("a", "b", "c");
		assertThat(result.universities().getFirst().places()).containsExactly(new TopPlace(SE, 1));
		assertThat(result.universities()).extracting(UniversityChoice::addedAsFill).containsOnly(false);
	}

	@Test
	void ranksUniversitiesByHitsThenPlacesThenFirstPlaces() {
		Result result = generate(List.of(SE, AMI),
				// SE: x, y, z;  AMI: y, z, w
				program("x-se", "x", SE, 0.99),
				program("y-se", "y", SE, 0.98),
				program("z-se", "z", SE, 0.97),
				program("y-ami", "y", AMI, 0.99),
				program("z-ami", "z", AMI, 0.98),
				program("w-ami", "w", AMI, 0.97));

		// y: 2 попадания, сумма 3; z: 2 попадания, сумма 5; x и w: по одному, x выше по первым местам.
		assertThat(result.universities()).extracting(UniversityChoice::universityId).containsExactly("y", "z", "x", "w");
		UniversityChoice y = result.universities().getFirst();
		assertThat(List.of(y.hits(), y.sumOfPlaces(), y.firstPlaces())).containsExactly(2, 3, 1);
	}

	@Test
	void comparesPassingScoresOnRelativeScale() {
		// Проходной 350 из 400 (0.875) ниже, чем 270 из 300 (0.9).
		Result result = generate(List.of(SE),
				program("four-exams", "four", SE, 350 / 400.0),
				program("three-exams", "three", SE, 270 / 300.0));

		assertThat(result.universities()).extracting(UniversityChoice::universityId).containsExactly("three", "four");
	}

	@Test
	void programsWithoutPassingScoreGoLastAndBviDoesNotBoost() {
		Result result = generate(List.of(SE),
				new Candidate("bvi-se", "bvi", SE, true, true, 0.70),
				new Candidate("unknown-se", "unknown", SE, false, true, null),
				program("strong-se", "strong", SE, 0.90));

		assertThat(result.universities()).extracting(UniversityChoice::universityId)
				.containsExactly("strong", "bvi", "unknown");
	}

	@Test
	void fillsUpToFiveUniversitiesFromNextPositions() {
		List<Candidate> candidates = new ArrayList<>();
		for (int i = 1; i <= 7; i++) {
			candidates.add(program("u" + i + "-se", "u" + i, SE, 1.0 - i / 100.0));
		}

		Result result = generate(List.of(SE), candidates.toArray(Candidate[]::new));

		assertThat(result.universities()).extracting(UniversityChoice::universityId)
				.containsExactly("u1", "u2", "u3", "u4", "u5");
		assertThat(result.universities()).extracting(UniversityChoice::addedAsFill)
				.containsExactly(false, false, false, true, true);
	}

	@Test
	void neverPadsPlanWithIneligiblePrograms() {
		Result result = generate(List.of(SE),
				program("ok-se", "ok", SE, 0.9),
				new Candidate("no-se", "no", SE, false, false, 0.99));

		assertThat(result.universities()).extracting(UniversityChoice::universityId).containsExactly("ok");
	}

	@Test
	void fillsDirectionsSelectedThenRelatedThenOthers() {
		Result result = generator.generate(List.of(SE, AMI), List.of(SE, AMI), Set.of(IS),
				List.of(
						program("a-law", "a", LAW, 0.99),
						program("a-is", "a", IS, 0.60),
						program("a-ami", "a", AMI, 0.70),
						program("a-se", "a", SE, 0.80)),
				List.of());

		assertThat(result.universities().getFirst().programs()).containsExactly(
				new ProgramChoice("a-se", Reason.SELECTED_DIRECTION),
				new ProgramChoice("a-ami", Reason.SELECTED_DIRECTION),
				new ProgramChoice("a-is", Reason.RELATED_DIRECTION),
				new ProgramChoice("a-law", Reason.AUTO_FILL));
	}

	@Test
	void limitsToFiveProgramsWithOneProgramPerDirection() {
		List<Candidate> candidates = new ArrayList<>(List.of(
				program("a-se", "a", SE, 0.99),
				program("a-se-second", "a", SE, 0.98)));
		for (int i = 1; i <= 6; i++) {
			candidates.add(program("a-d" + i, "a", "d" + i, 0.5));
		}

		Result result = generate(List.of(SE), candidates.toArray(Candidate[]::new));

		List<ProgramChoice> programs = result.universities().getFirst().programs();
		assertThat(programs).hasSize(5);
		assertThat(programs).extracting(ProgramChoice::programId).doesNotContain("a-se-second");
	}

	@Test
	void reportsDirectionsWithoutCandidatesAndBviOutsidePlan() {
		List<Candidate> candidates = new ArrayList<>();
		for (int i = 1; i <= 5; i++) {
			candidates.add(program("u" + i + "-se", "u" + i, SE, 0.9));
		}
		candidates.add(new Candidate("far-se", "far", SE, true, true, 0.5));

		Result result = generator.generate(List.of(SE, AMI), List.of(SE, AMI), Set.of(), candidates, List.of());

		assertThat(result.directionsWithoutCandidates()).containsExactly(AMI);
		assertThat(result.bviOutsidePlan()).containsExactly("far-se");
	}

	@Test
	void fillModeKeepsBasePlanFirst() {
		Result result = generator.generate(List.of(SE), List.of(SE), Set.of(),
				List.of(program("a-se", "a", SE, 0.99), program("b-se", "b", SE, 0.5), program("b-ami", "b", AMI, 0.4)),
				List.of(new BaseUniversity("b", List.of("b-ami"))));

		assertThat(result.universities()).extracting(UniversityChoice::universityId).containsExactly("b", "a");
		assertThat(result.universities().getFirst().retained()).isTrue();
		assertThat(result.universities().getFirst().programs()).containsExactly(
				new ProgramChoice("b-ami", Reason.RETAINED),
				new ProgramChoice("b-se", Reason.SELECTED_DIRECTION));
	}

	@Test
	void resultDoesNotDependOnCandidateOrder() {
		List<Candidate> candidates = new ArrayList<>(List.of(
				program("a-se", "a", SE, 0.9),
				program("b-se", "b", SE, 0.9),
				program("c-se", "c", SE, 0.8)));
		List<String> first = ids(generate(List.of(SE), candidates.toArray(Candidate[]::new)));
		Collections.reverse(candidates);

		assertThat(ids(generate(List.of(SE), candidates.toArray(Candidate[]::new)))).isEqualTo(first)
				.containsExactly("a", "b", "c");
	}

	private Result generate(List<String> directions, Candidate... candidates) {
		return generator.generate(directions, directions, Set.of(), List.of(candidates), List.of());
	}

	private static List<String> ids(Result result) {
		return result.universities().stream().map(UniversityChoice::universityId).toList();
	}

	private static Candidate program(String programId, String universityId, String directionId, double relative) {
		return new Candidate(programId, universityId, directionId, false, true, relative);
	}
}
