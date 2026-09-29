package com.runiversityadmisson.bot.domain.applicant.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.runiversityadmisson.bot.domain.applicant.service.ProgramRecommendationPolicy.Candidate;
import com.runiversityadmisson.bot.domain.applicant.service.ProgramRecommendationPolicy.Comparison;
import com.runiversityadmisson.bot.domain.applicant.service.ProgramRecommendationPolicy.Eligibility;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class ProgramRecommendationPolicyTest {

	private final ProgramRecommendationPolicy policy = new ProgramRecommendationPolicy(5);

	@Test
	void keepsNarrowWindowWhenEnoughPrograms() {
		List<Candidate> candidates = new ArrayList<>(IntStream.rangeClosed(1, 8)
				.mapToObj(i -> eligible("p" + i, i))
				.toList());
		candidates.add(eligible("far-below", -18));
		candidates.add(eligible("far-above", 28));

		assertThat(ids(select(candidates))).containsExactly("p8", "p7", "p6", "p5", "p4", "p3", "p2", "p1");
	}

	@Test
	void widensWindowOnceWhenFewerThanEight() {
		List<Candidate> candidates = List.of(
				eligible("in", 0),
				eligible("wide-below", -20),
				eligible("wide-above", 30),
				eligible("outside-below", -21),
				eligible("outside-above", 31));

		assertThat(ids(select(candidates))).containsExactly("wide-above", "in", "wide-below");
	}

	@Test
	void windowBoundariesAreInclusive() {
		List<Candidate> candidates = new ArrayList<>(IntStream.rangeClosed(1, 6)
				.mapToObj(i -> eligible("filler" + i, 0))
				.toList());
		candidates.add(eligible("lower", -15));
		candidates.add(eligible("upper", 25));
		candidates.add(eligible("below", -16));
		candidates.add(eligible("above", 26));

		assertThat(ids(select(candidates))).contains("lower", "upper").doesNotContain("below", "above");
	}

	@Test
	void bviAlwaysComesFirstEvenFarBelowPassingScore() {
		List<Candidate> candidates = List.of(
				eligible("regular", 10),
				new Candidate("u", "bvi-risky", true, Eligibility.INELIGIBLE, 200, null),
				new Candidate("u", "bvi", true, Eligibility.ELIGIBLE, 240, -60));

		assertThat(ids(select(candidates))).containsExactly("bvi", "bvi-risky", "regular");
	}

	@Test
	void showsAllBviProgramsEvenAboveLimit() {
		List<Candidate> candidates = new ArrayList<>(IntStream.rangeClosed(1, 12)
				.mapToObj(i -> new Candidate("u", "bvi" + i, true, Eligibility.ELIGIBLE, 250, i))
				.toList());
		candidates.add(eligible("regular", 0));

		assertThat(select(candidates)).hasSize(12).allMatch(Candidate::bviAvailable);
	}

	@Test
	void limitsToTenPrograms() {
		List<Candidate> candidates = IntStream.rangeClosed(1, 15)
				.mapToObj(i -> eligible("p" + i, i))
				.toList();

		assertThat(select(candidates)).hasSize(10).first().extracting(Candidate::programId).isEqualTo("p15");
	}

	@Test
	void dropsProgramsWithoutAdmission() {
		List<Candidate> candidates = List.of(
				new Candidate("u", "incomplete", false, Eligibility.INCOMPLETE, null, null),
				new Candidate("u", "ineligible", false, Eligibility.INELIGIBLE, 250, null),
				eligible("ok", 0));

		assertThat(ids(select(candidates))).containsExactly("ok");
	}

	@Test
	void putsProgramsWithoutHistoryAfterWindow() {
		List<Candidate> candidates = List.of(
				new Candidate("u", "no-history", false, Eligibility.ELIGIBLE, 270, null),
				eligible("history", -10));

		assertThat(ids(select(candidates))).containsExactly("history", "no-history");
	}

	@Test
	void orderDoesNotDependOnInputOrder() {
		List<Candidate> candidates = new ArrayList<>(List.of(
				new Candidate("b", "b1", false, Eligibility.ELIGIBLE, 260, 5),
				new Candidate("a", "a2", false, Eligibility.ELIGIBLE, 260, 5),
				new Candidate("a", "a1", false, Eligibility.ELIGIBLE, 260, 5),
				new Candidate("c", "c1", false, Eligibility.ELIGIBLE, 270, 5)));
		List<String> expected = List.of("c1", "a1", "a2", "b1");

		assertThat(ids(select(candidates))).isEqualTo(expected);
		Collections.reverse(candidates);
		assertThat(ids(select(candidates))).isEqualTo(expected);
	}

	@Test
	void comparesWithPreviousYear() {
		assertThat(policy.compare(eligible("p", 6))).isEqualTo(Comparison.ABOVE_PREVIOUS);
		assertThat(policy.compare(eligible("p", 5))).isEqualTo(Comparison.NEAR_PREVIOUS);
		assertThat(policy.compare(eligible("p", -5))).isEqualTo(Comparison.NEAR_PREVIOUS);
		assertThat(policy.compare(eligible("p", -6))).isEqualTo(Comparison.BELOW_PREVIOUS);
		assertThat(policy.compare(new Candidate("u", "p", false, Eligibility.ELIGIBLE, 250, null)))
				.isEqualTo(Comparison.INSUFFICIENT_DATA);
		assertThat(policy.compare(new Candidate("u", "p", false, Eligibility.INELIGIBLE, 250, null)))
				.isEqualTo(Comparison.INELIGIBLE);
		assertThat(policy.compare(new Candidate("u", "p", true, Eligibility.INELIGIBLE, 250, null)))
				.isEqualTo(Comparison.BVI);
	}

	private List<Candidate> select(List<Candidate> candidates) {
		return policy.select(candidates, Function.identity());
	}

	private static List<String> ids(List<Candidate> candidates) {
		return candidates.stream().map(Candidate::programId).toList();
	}

	private static Candidate eligible(String programId, int difference) {
		return new Candidate("u", programId, false, Eligibility.ELIGIBLE, 250 + difference, difference);
	}
}
