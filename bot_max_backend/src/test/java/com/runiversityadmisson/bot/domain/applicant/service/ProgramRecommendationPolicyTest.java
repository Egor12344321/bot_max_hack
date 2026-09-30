package com.runiversityadmisson.bot.domain.applicant.service;

import static org.assertj.core.api.Assertions.assertThat;
import com.runiversityadmisson.bot.domain.applicant.service.ProgramRecommendationPolicy.*;
import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class ProgramRecommendationPolicyTest {
	private final ProgramRecommendationPolicy policy = new ProgramRecommendationPolicy(5);

	@Test
	void configurableInclusiveDeficitAndNoUpperBound() {
		List<Candidate> items = List.of(candidate("safe", 100), candidate("boundary", -15), candidate("risky", -16));
		assertThat(select(items, 15)).extracting(Candidate::programId).containsExactly("boundary", "safe");
		assertThat(select(items, 10)).extracting(Candidate::programId).containsExactly("safe");
		assertThat(select(items, 20)).extracting(Candidate::programId).containsExactly("risky", "boundary", "safe");
	}

	@Test
	void bviBypassesAdmissionButDoesNotOutrankHigherPassingScore() {
		var bvi = new Candidate("u", "bvi", true, Eligibility.INCOMPLETE, null, null, 200, 300);
		var noAdmission = new Candidate("u", "missing", false, Eligibility.INCOMPLETE, null, null, 290, 300);
		assertThat(select(List.of(bvi, noAdmission, candidate("regular", -10)), 15))
				.extracting(Candidate::programId).containsExactly("regular", "bvi");
	}

	@Test
	void noHistoryIsLastAndSelectionDoesNotTruncateCandidates() {
		var items = new java.util.ArrayList<>(IntStream.range(0, 30).mapToObj(i -> candidate("p" + i, i)).toList());
		items.add(new Candidate("u", "unknown", false, Eligibility.ELIGIBLE, 260, null, null, 300));
		assertThat(select(items, 15)).hasSize(31).last().extracting(Candidate::programId).isEqualTo("unknown");
	}

	@Test
	void differentExamScalesDoNotGiveFourExamsAnAutomaticAdvantage() {
		var three = new Candidate("u", "three", true, Eligibility.ELIGIBLE, 290, 0, 290, 300);
		var four = new Candidate("u", "four", true, Eligibility.ELIGIBLE, 320, 0, 320, 400);
		assertThat(select(List.of(four, three), 15)).containsExactly(three, four);
	}

	@Test
	void orderingIsStableAndComparisonStillDescribesTheApplicant() {
		assertThat(select(List.of(candidate("b", 0), candidate("a", 0)), 0))
				.extracting(Candidate::programId).containsExactly("a", "b");
		assertThat(policy.compare(candidate("p", -5))).isEqualTo(Comparison.NEAR_PREVIOUS);
		assertThat(policy.compare(candidate("p", 6))).isEqualTo(Comparison.ABOVE_PREVIOUS);
		assertThat(policy.compare(candidate("p", -6))).isEqualTo(Comparison.BELOW_PREVIOUS);
		assertThat(policy.compare(new Candidate("u", "b", true, Eligibility.INELIGIBLE, 200, null)))
				.isEqualTo(Comparison.BVI);
	}

	private Candidate candidate(String id, int delta) {
		return new Candidate("u", id, false, Eligibility.ELIGIBLE, 260, delta);
	}
	private List<Candidate> select(List<Candidate> items, int deficit) {
		return policy.select(items, Function.identity(), deficit);
	}
}
