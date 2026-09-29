package com.runiversityadmisson.bot.domain.applicant.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Автоплан 5×5 по согласованной спецификации. Без Spring и БД; ничего не сохраняет.
 *
 * <ol>
 *   <li>Программы внутри направления ранжируются по относительному прошлогоднему проходному
 *       (проходной / максимум суммы испытаний) по убыванию; без проходного — в конце; затем id программы.
 *       БВИ не поднимает программу в рейтинге.</li>
 *   <li>По каждому направлению берутся первые три разных вуза и их места 1–3.</li>
 *   <li>Вузы сортируются: попаданий в топ-3 больше → сумма мест меньше → первых мест больше → id вуза.
 *       Берутся первые пять, затем добор следующими уникальными вузами по позициям рекомендаций.</li>
 *   <li>Внутри вуза до пяти направлений: выбранные пользователем (в его порядке), затем смежные
 *       (общая категория интересов с выбранными), затем прочие подходящие; одна программа на направление.</li>
 * </ol>
 * Если передан базовый план (режим «дополнить»), его вузы и программы сохраняются первыми в том же порядке.
 */
public class ApplicationPlanGenerator {

	public static final int MAX_UNIVERSITIES = 5;
	public static final int MAX_PROGRAMS = 5;
	static final int TOP_UNIVERSITIES_PER_DIRECTION = 3;

	private static final Comparator<Candidate> RANK = Comparator
			.comparing(Candidate::relativePassingScore, Comparator.nullsLast(Comparator.reverseOrder()))
			.thenComparing(Candidate::programId);

	/**
	 * @param rankingDirections направления, по которым выбираются вузы (обычно выбранные пользователем)
	 * @param selectedDirections выбранные пользователем направления в его порядке
	 * @param relatedDirections  невыбранные направления с общей категорией интересов
	 * @param candidates         все рассчитанные программы, в том числе неподходящие
	 * @param basePlan           план, который нужно дополнить; пустой — генерация с нуля
	 */
	public Result generate(List<String> rankingDirections, List<String> selectedDirections,
			Set<String> relatedDirections, List<Candidate> candidates, List<BaseUniversity> basePlan) {
		Map<String, Candidate> byId = candidates.stream()
				.collect(Collectors.toMap(Candidate::programId, candidate -> candidate, (a, b) -> a, LinkedHashMap::new));
		List<Candidate> eligible = candidates.stream().filter(Candidate::eligible).sorted(RANK).toList();
		Map<String, List<Candidate>> ranked = new LinkedHashMap<>();
		for (String direction : rankingDirections) {
			ranked.put(direction, eligible.stream().filter(c -> c.directionId().equals(direction)).toList());
		}

		Map<String, Stats> stats = topThree(rankingDirections, ranked);
		Map<String, Builder> chosen = new LinkedHashMap<>();
		for (BaseUniversity base : basePlan) {
			chosen.put(base.universityId(), new Builder(base.universityId(), stats.get(base.universityId()), false, true));
		}
		stats.values().stream()
				.sorted(Comparator.comparingInt(Stats::hits).reversed()
						.thenComparingInt(Stats::sumOfPlaces)
						.thenComparing(Comparator.comparingInt(Stats::firstPlaces).reversed())
						.thenComparing(Stats::universityId))
				.forEach(entry -> {
					if (chosen.size() < MAX_UNIVERSITIES && !chosen.containsKey(entry.universityId())) {
						chosen.put(entry.universityId(), new Builder(entry.universityId(), entry, false, false));
					}
				});
		fillUniversities(rankingDirections, ranked, stats, chosen);

		Map<String, List<String>> baseByUniversity = basePlan.stream()
				.collect(Collectors.toMap(BaseUniversity::universityId, BaseUniversity::programIds));
		List<UniversityChoice> universities = chosen.values().stream()
				.map(builder -> builder.build(choosePrograms(builder.universityId,
						baseByUniversity.getOrDefault(builder.universityId, List.of()),
						selectedDirections, relatedDirections, eligible, byId)))
				.toList();

		Set<String> inPlan = universities.stream()
				.flatMap(university -> university.programs().stream())
				.map(ProgramChoice::programId)
				.collect(Collectors.toSet());
		List<String> directionsWithoutCandidates = selectedDirections.stream()
				.filter(direction -> eligible.stream().noneMatch(c -> c.directionId().equals(direction)))
				.toList();
		List<String> bviOutsidePlan = candidates.stream()
				.filter(Candidate::bvi)
				.filter(candidate -> !inPlan.contains(candidate.programId()))
				.sorted(RANK)
				.map(Candidate::programId)
				.toList();
		return new Result(universities, directionsWithoutCandidates, bviOutsidePlan);
	}

	/** Первые три разных вуза в рейтинге каждого направления. */
	private static Map<String, Stats> topThree(List<String> directions, Map<String, List<Candidate>> ranked) {
		Map<String, Stats> stats = new LinkedHashMap<>();
		for (String direction : directions) {
			Set<String> seen = new LinkedHashSet<>();
			for (Candidate candidate : ranked.get(direction)) {
				if (seen.size() == TOP_UNIVERSITIES_PER_DIRECTION) {
					break;
				}
				if (seen.add(candidate.universityId())) {
					stats.computeIfAbsent(candidate.universityId(), Stats::new)
							.add(new TopPlace(direction, seen.size()));
				}
			}
		}
		return stats;
	}

	/** Добор: следующие позиции рекомендаций по направлениям по очереди, пока вузов меньше пяти. */
	private static void fillUniversities(List<String> directions, Map<String, List<Candidate>> ranked,
			Map<String, Stats> stats, Map<String, Builder> chosen) {
		int longest = ranked.values().stream().mapToInt(List::size).max().orElse(0);
		for (int position = 0; position < longest && chosen.size() < MAX_UNIVERSITIES; position++) {
			for (String direction : directions) {
				List<Candidate> list = ranked.get(direction);
				if (position >= list.size() || chosen.size() >= MAX_UNIVERSITIES) {
					continue;
				}
				String university = list.get(position).universityId();
				if (!chosen.containsKey(university)) {
					chosen.put(university, new Builder(university, stats.get(university), true, false));
				}
			}
		}
	}

	private static List<ProgramChoice> choosePrograms(String universityId, List<String> baseProgramIds,
			List<String> selectedDirections, Set<String> relatedDirections, List<Candidate> eligible,
			Map<String, Candidate> byId) {
		List<ProgramChoice> programs = new ArrayList<>();
		Set<String> takenDirections = new HashSet<>();
		for (String programId : baseProgramIds) {
			programs.add(new ProgramChoice(programId, Reason.RETAINED));
			Candidate known = byId.get(programId);
			if (known != null) {
				takenDirections.add(known.directionId());
			}
		}
		List<Candidate> ofUniversity = eligible.stream()
				.filter(candidate -> candidate.universityId().equals(universityId))
				.toList();
		for (String direction : selectedDirections) {
			ofUniversity.stream()
					.filter(candidate -> candidate.directionId().equals(direction))
					.findFirst()
					.ifPresent(candidate -> add(programs, takenDirections, candidate, Reason.SELECTED_DIRECTION));
		}
		ofUniversity.stream()
				.filter(candidate -> relatedDirections.contains(candidate.directionId()))
				.forEach(candidate -> add(programs, takenDirections, candidate, Reason.RELATED_DIRECTION));
		ofUniversity.forEach(candidate -> add(programs, takenDirections, candidate, Reason.AUTO_FILL));
		return programs;
	}

	private static void add(List<ProgramChoice> programs, Set<String> takenDirections, Candidate candidate,
			Reason reason) {
		if (programs.size() < MAX_PROGRAMS && takenDirections.add(candidate.directionId())) {
			programs.add(new ProgramChoice(candidate.programId(), reason));
		}
	}

	/**
	 * Программа, рассчитанная для пользователя.
	 *
	 * @param eligible             может участвовать в автоплане: БВИ или допуск и дефицит не больше допустимого
	 * @param relativePassingScore прошлогодний проходной / максимум суммы испытаний; null — нет данных
	 */
	public record Candidate(String programId, String universityId, String directionId, boolean bvi,
			boolean eligible, Double relativePassingScore) {
	}

	/** Вуз базового плана и его программы в порядке приоритета. */
	public record BaseUniversity(String universityId, List<String> programIds) {
	}

	public enum Reason {
		/** Выбранное пользователем направление. */
		SELECTED_DIRECTION,
		/** Смежное направление: общая категория интересов с выбранными. */
		RELATED_DIRECTION,
		/** Добор подходящего направления. */
		AUTO_FILL,
		/** Позиция из плана пользователя. */
		RETAINED
	}

	public record ProgramChoice(String programId, Reason reason) {
	}

	/** Место вуза в топ-3 направления (1–3). */
	public record TopPlace(String directionId, int place) {
	}

	/**
	 * @param addedAsFill вуз не попал в топ-3 и добавлен при доборе
	 * @param retained    вуз из плана пользователя
	 */
	public record UniversityChoice(String universityId, List<TopPlace> places, int hits, int sumOfPlaces,
			int firstPlaces, boolean addedAsFill, boolean retained, List<ProgramChoice> programs) {
	}

	/**
	 * @param directionsWithoutCandidates выбранные направления без подходящих программ
	 * @param bviOutsidePlan              программы с доступным БВИ, не попавшие в план
	 */
	public record Result(List<UniversityChoice> universities, List<String> directionsWithoutCandidates,
			List<String> bviOutsidePlan) {
	}

	private static final class Stats {
		private final String universityId;
		private final List<TopPlace> places = new ArrayList<>();

		Stats(String universityId) {
			this.universityId = universityId;
		}

		void add(TopPlace place) {
			places.add(place);
		}

		String universityId() {
			return universityId;
		}

		int hits() {
			return places.size();
		}

		int sumOfPlaces() {
			return places.stream().mapToInt(TopPlace::place).sum();
		}

		int firstPlaces() {
			return (int) places.stream().filter(place -> place.place() == 1).count();
		}
	}

	private static final class Builder {
		private final String universityId;
		private final Stats stats;
		private final boolean addedAsFill;
		private final boolean retained;

		Builder(String universityId, Stats stats, boolean addedAsFill, boolean retained) {
			this.universityId = universityId;
			this.stats = stats;
			this.addedAsFill = addedAsFill;
			this.retained = retained;
		}

		UniversityChoice build(List<ProgramChoice> programs) {
			return stats == null
					? new UniversityChoice(universityId, List.of(), 0, 0, 0, addedAsFill, retained, programs)
					: new UniversityChoice(universityId, List.copyOf(stats.places), stats.hits(), stats.sumOfPlaces(),
							stats.firstPlaces(), addedAsFill, retained, programs);
		}
	}
}
