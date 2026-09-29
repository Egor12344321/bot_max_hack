package com.runiversityadmisson.bot.domain.applicant.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/**
 * Отбор и ранжирование программ для экрана «Рекомендации по направлению». Без Spring и БД.
 *
 * <ul>
 *   <li>программы без допуска (нет ЕГЭ или он ниже порога) не показываются;</li>
 *   <li>программы с БВИ показываются всегда и идут первыми;</li>
 *   <li>остальные — только если разница с прошлогодним проходным в окне −15…+25,
 *       а если вместе с БВИ набралось меньше 8, окно один раз расширяется до −20…+30;</li>
 *   <li>внутри окна — по разнице по убыванию, затем программы без прошлогодних данных;</li>
 *   <li>в выдаче не больше 10 программ, кроме случая, когда программ с БВИ больше.</li>
 * </ul>
 * При равенстве: конкурсный балл по убыванию, затем id вуза и id программы.
 */
public class ProgramRecommendationPolicy {

	static final Window NARROW = new Window(-15, 25);
	static final Window WIDE = new Window(-20, 30);
	static final int MIN_RESULTS = 8;
	static final int MAX_RESULTS = 10;

	private static final Comparator<Candidate> RANK = Comparator
			.comparing(Candidate::scoreDifference, Comparator.nullsLast(Comparator.reverseOrder()))
			.thenComparing(Candidate::totalScore, Comparator.nullsLast(Comparator.reverseOrder()))
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
		List<T> bvi = sorted(items.stream().filter(item -> view.apply(item).bviAvailable()).toList(), view);
		List<T> eligible = items.stream()
				.filter(item -> !view.apply(item).bviAvailable())
				.filter(item -> view.apply(item).eligibility() == Eligibility.ELIGIBLE)
				.toList();
		List<T> withoutData = sorted(eligible.stream()
				.filter(item -> view.apply(item).scoreDifference() == null)
				.toList(), view);

		List<T> inWindow = inWindow(eligible, view, NARROW);
		if (bvi.size() + inWindow.size() < MIN_RESULTS) {
			inWindow = inWindow(eligible, view, WIDE);
		}

		List<T> result = new ArrayList<>(bvi);
		result.addAll(inWindow);
		result.addAll(withoutData);
		return result.subList(0, Math.min(result.size(), Math.max(MAX_RESULTS, bvi.size())));
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

	private static <T> List<T> inWindow(List<T> eligible, Function<T, Candidate> view, Window window) {
		return sorted(eligible.stream()
				.filter(item -> window.contains(view.apply(item).scoreDifference()))
				.toList(), view);
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
			Integer totalScore, Integer scoreDifference) {
	}

	record Window(int from, int to) {

		boolean contains(Integer difference) {
			return difference != null && difference >= from && difference <= to;
		}
	}
}
