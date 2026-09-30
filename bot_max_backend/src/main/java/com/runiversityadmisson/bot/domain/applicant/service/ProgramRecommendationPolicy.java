package com.runiversityadmisson.bot.domain.applicant.service;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/**
 * Отбор и ранжирование программ для экрана «Рекомендации по направлению». Без Spring и БД.
 *
 * <ul>
 *   <li>без допуска программы исключаются, кроме подтверждённого БВИ;</li>
 *   <li>допустимый дефицит настраивается; верхней границы запаса нет;</li>
 *   <li>проходной по убыванию, без автоматического приоритета БВИ;</li>
 *   <li>неизвестный проходной — в конце; пагинация применяется вызывающим кодом.</li>
 * </ul>
 * При равенстве: id вуза и id программы. Лимит экрана применяется после ранжирования в сервисе.
 */
public class ProgramRecommendationPolicy {

	private static final Comparator<Candidate> RANK = Comparator
			.comparing(Candidate::relativePassingScore, Comparator.nullsLast(Comparator.reverseOrder()))
			.thenComparing(Candidate::universityId)
			.thenComparing(Candidate::programId);

	private final int nearThreshold;

	/**
	 * @param nearThreshold |разница| не больше порога — «около прошлогоднего проходного»
	 */
	public ProgramRecommendationPolicy(int nearThreshold) {
		this.nearThreshold = nearThreshold;
	}

	public <T> List<T> select(List<T> items, Function<T, Candidate> view) {
		return select(items, view, 15);
	}

	public <T> List<T> select(List<T> items, Function<T, Candidate> view, int maxScoreDeficit) {
		if (maxScoreDeficit < 0) throw new IllegalArgumentException("Negative score deficit");
		return sorted(items.stream().filter(item -> {
			Candidate candidate = view.apply(item);
			return candidate.bviAvailable() || candidate.eligibility() == Eligibility.ELIGIBLE
					&& (candidate.scoreDifference() == null || candidate.scoreDifference() >= -maxScoreDeficit);
		}).toList(), view);
	}

	public Comparison compare(Candidate candidate) {
		if (candidate.bviAvailable()) {
			return Comparison.BVI;
		}
		if (candidate.eligibility() == Eligibility.INELIGIBLE) {
			return Comparison.INELIGIBLE;
		}
		Integer difference = candidate.scoreDifference();
		if (difference == null) {
			return Comparison.INSUFFICIENT_DATA;
		}
		if (Math.abs(difference) <= nearThreshold) {
			return Comparison.NEAR_PREVIOUS;
		}
		return difference > 0 ? Comparison.ABOVE_PREVIOUS : Comparison.BELOW_PREVIOUS;
	}

	private static <T> List<T> sorted(List<T> items, Function<T, Candidate> view) {
		return items.stream().sorted(Comparator.comparing(view, RANK)).toList();
	}

	public enum Eligibility {
		ELIGIBLE("eligible"),
		INCOMPLETE("incomplete"),
		INELIGIBLE("ineligible");

		private final String code;

		Eligibility(String code) {
			this.code = code;
		}

		public String getCode() {
			return code;
		}
	}

	public enum Comparison {
		BVI("bvi"),
		ABOVE_PREVIOUS("above_previous"),
		NEAR_PREVIOUS("near_previous"),
		BELOW_PREVIOUS("below_previous"),
		INSUFFICIENT_DATA("insufficient_data"),
		INELIGIBLE("ineligible");

		private final String code;

		Comparison(String code) {
			this.code = code;
		}

		public String getCode() {
			return code;
		}
	}

	/**
	 * @param scoreDifference конкурсный балл минус прошлогодний проходной; null — сравнить нельзя
	 */
	public record Candidate(String universityId, String programId, boolean bviAvailable, Eligibility eligibility,
			Integer totalScore, Integer scoreDifference, Integer passingScore, int entranceScoreMax) {
		public Candidate(String universityId, String programId, boolean bviAvailable, Eligibility eligibility,
				Integer totalScore, Integer scoreDifference) {
			this(universityId, programId, bviAvailable, eligibility, totalScore, scoreDifference,
					totalScore == null || scoreDifference == null ? null : totalScore - scoreDifference, 300);
		}
		public Double relativePassingScore() {
			return passingScore == null || entranceScoreMax <= 0 ? null : (double) passingScore / entranceScoreMax;
		}
	}
}
