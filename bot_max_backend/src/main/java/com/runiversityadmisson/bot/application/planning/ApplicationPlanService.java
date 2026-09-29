package com.runiversityadmisson.bot.application.planning;

import com.runiversityadmisson.bot.application.dto.planning.ApplicationPlanResponse;
import com.runiversityadmisson.bot.application.dto.planning.PlanCompositionDto;
import com.runiversityadmisson.bot.application.dto.planning.PlanExplanationResponse;
import com.runiversityadmisson.bot.application.dto.planning.PlanPreviewResponse;
import com.runiversityadmisson.bot.application.dto.planning.PlanTopPlaceResponse;
import com.runiversityadmisson.bot.application.dto.planning.PlanUniversityDto;
import com.runiversityadmisson.bot.application.dto.planning.PlanUniversityRankingResponse;
import com.runiversityadmisson.bot.application.dto.planning.PreviewApplicationPlanRequest;
import com.runiversityadmisson.bot.application.dto.planning.ProgramOptionResponse;
import com.runiversityadmisson.bot.application.dto.planning.SaveApplicationPlanRequest;
import com.runiversityadmisson.bot.domain.applicant.model.direction.StudyDirection;
import com.runiversityadmisson.bot.domain.applicant.model.planning.ApplicationPlan;
import com.runiversityadmisson.bot.domain.applicant.model.planning.ApplicationPlanItem;
import com.runiversityadmisson.bot.domain.applicant.model.profile.User;
import com.runiversityadmisson.bot.domain.applicant.model.university.Program;
import com.runiversityadmisson.bot.domain.applicant.ports.direction.StudyDirectionRepository;
import com.runiversityadmisson.bot.domain.applicant.ports.planning.ApplicationPlanRepository;
import com.runiversityadmisson.bot.domain.applicant.ports.profile.UserRepository;
import com.runiversityadmisson.bot.domain.applicant.ports.university.ProgramRepository;
import com.runiversityadmisson.bot.domain.applicant.service.ApplicationPlanGenerator;
import com.runiversityadmisson.bot.domain.applicant.service.ApplicationPlanGenerator.BaseUniversity;
import com.runiversityadmisson.bot.domain.applicant.service.ApplicationPlanGenerator.Candidate;
import com.runiversityadmisson.bot.domain.applicant.service.ApplicationPlanGenerator.ProgramChoice;
import com.runiversityadmisson.bot.domain.applicant.service.ApplicationPlanGenerator.Result;
import com.runiversityadmisson.bot.domain.applicant.service.ApplicationPlanGenerator.UniversityChoice;
import com.runiversityadmisson.bot.presentation.exception.BadRequestException;
import com.runiversityadmisson.bot.presentation.exception.ConflictException;
import com.runiversityadmisson.bot.presentation.exception.ResourceNotFoundException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * План поступления 5×5: чтение с пересчётом, сохранение с проверкой версии и черновик автоплана.
 * Автоплан только предлагает состав; сохраняет его пользователь отдельным PUT.
 */
@Service
public class ApplicationPlanService {

	static final Set<Integer> ALLOWED_DEFICITS = Set.of(0, 10, 15, 20);
	static final int DEFAULT_DEFICIT = 15;
	static final String CONFLICT_MESSAGE =
			"План изменился в другом окне. Загрузите актуальную версию и повторите действие.";

	private final UserRepository userRepository;
	private final StudyDirectionRepository directionRepository;
	private final ProgramRepository programRepository;
	private final ApplicationPlanRepository planRepository;
	private final RecommendationService recommendationService;
	private final ApplicationPlanGenerator generator = new ApplicationPlanGenerator();
	private final int nearPreviousThreshold;

	public ApplicationPlanService(UserRepository userRepository, StudyDirectionRepository directionRepository,
			ProgramRepository programRepository, ApplicationPlanRepository planRepository,
			RecommendationService recommendationService,
			@Value("${admission.near-previous-threshold:5}") int nearPreviousThreshold) {
		this.userRepository = userRepository;
		this.directionRepository = directionRepository;
		this.programRepository = programRepository;
		this.planRepository = planRepository;
		this.recommendationService = recommendationService;
		this.nearPreviousThreshold = nearPreviousThreshold;
	}

	@Transactional(readOnly = true)
	public ApplicationPlanResponse getPlan(UUID sessionId) {
		getUser(sessionId);
		Evaluation evaluation = evaluate(sessionId);
		return planRepository.findById(sessionId)
				.map(plan -> toResponse(toComposition(plan), evaluation, plan.getVersion(), plan.getSavedAt()))
				.orElseGet(() -> toResponse(new PlanCompositionDto(List.of(), null), evaluation, 0, null));
	}

	@Transactional
	public ApplicationPlanResponse savePlan(UUID sessionId, SaveApplicationPlanRequest request) {
		getUser(sessionId);
		Evaluation evaluation = evaluate(sessionId);
		PlanCompositionDto composition = new PlanCompositionDto(request.universities(), request.bviProgramId());
		validate(composition, evaluation, true);

		ApplicationPlan plan = planRepository.findForUpdate(sessionId).orElse(null);
		int currentVersion = plan == null ? 0 : plan.getVersion();
		if (request.expectedVersion() != currentVersion) {
			throw new ConflictException(CONFLICT_MESSAGE);
		}
		if (plan == null) {
			plan = new ApplicationPlan();
			plan.setUserId(sessionId);
		}
		plan.setVersion(currentVersion + 1);
		plan.setBviProgramId(composition.bviProgramId());
		plan.setSavedAt(OffsetDateTime.now(ZoneOffset.UTC));
		plan.getItems().clear();
		for (int universityIndex = 0; universityIndex < composition.universities().size(); universityIndex++) {
			PlanUniversityDto university = composition.universities().get(universityIndex);
			for (int programIndex = 0; programIndex < university.programIds().size(); programIndex++) {
				plan.getItems().add(new ApplicationPlanItem(university.universityId(),
						university.programIds().get(programIndex), universityIndex, programIndex));
			}
		}
		try {
			planRepository.saveAndFlush(plan);
		} catch (DataIntegrityViolationException exception) {
			// Параллельно сохранили первый план этого пользователя.
			throw new ConflictException(CONFLICT_MESSAGE);
		}
		return toResponse(composition, evaluation, plan.getVersion(), plan.getSavedAt());
	}

	@Transactional(readOnly = true)
	public PlanPreviewResponse preview(UUID sessionId, PreviewApplicationPlanRequest request) {
		User user = getUser(sessionId);
		boolean fill = parseMode(request);
		int deficit = request.allowedDeficit() == null ? DEFAULT_DEFICIT : request.allowedDeficit();
		if (!ALLOWED_DEFICITS.contains(deficit)) {
			throw new BadRequestException("Допустимое отставание от проходного: 0, 10, 15 или 20 баллов");
		}
		List<String> selected = List.copyOf(user.getDirectionIds());
		if (!fill && selected.isEmpty()) {
			throw new BadRequestException("Сначала выберите направления");
		}

		Evaluation evaluation = evaluate(sessionId);
		PlanCompositionDto base = fill ? request.basePlan() : new PlanCompositionDto(List.of(), null);
		validate(new PlanCompositionDto(base.universities(), null), evaluation, false);

		Map<String, StudyDirection> directions = directionRepository.findAll().stream()
				.collect(Collectors.toMap(StudyDirection::getId, Function.identity()));
		List<Candidate> candidates = evaluation.programs().values().stream()
				.map(program -> candidate(program, evaluation.options().get(program.getId()), deficit))
				.toList();
		List<String> rankingDirections = selected.isEmpty()
				? candidates.stream().filter(Candidate::eligible).map(Candidate::directionId).distinct().sorted().toList()
				: selected;
		Result result = generator.generate(rankingDirections, selected, related(selected, directions), candidates,
				base.universities().stream()
						.map(university -> new BaseUniversity(university.universityId(), university.programIds()))
						.toList());

		List<PlanUniversityDto> universities = result.universities().stream()
				.map(university -> new PlanUniversityDto(university.universityId(),
						university.programs().stream().map(ProgramChoice::programId).toList()))
				.toList();
		Set<String> inPlan = universities.stream()
				.flatMap(university -> university.programIds().stream())
				.collect(Collectors.toSet());
		List<String> warnings = new ArrayList<>();
		String bviProgramId = keptBvi(base.bviProgramId(), inPlan, evaluation, warnings);
		PlanCompositionDto composition = new PlanCompositionDto(universities, bviProgramId);
		warnings.addAll(previewWarnings(result, composition, evaluation, directions, deficit, base));

		return new PlanPreviewResponse(
				composition,
				options(composition, evaluation),
				explanations(result, evaluation, directions),
				ranking(result, evaluation, directions),
				warnings,
				OffsetDateTime.now(ZoneOffset.UTC),
				nearPreviousThreshold);
	}

	private static boolean parseMode(PreviewApplicationPlanRequest request) {
		return switch (request.mode()) {
			case "generate" -> {
				if (request.basePlan() != null) {
					throw new BadRequestException("Для режима generate basePlan не передаётся");
				}
				yield false;
			}
			case "fill" -> {
				if (request.basePlan() == null) {
					throw new BadRequestException("Для режима fill нужен basePlan");
				}
				yield true;
			}
			default -> throw new BadRequestException("mode должен быть generate или fill");
		};
	}

	/**
	 * Проверки состава. requireAvailable — для сохранения: программа должна быть доступна
	 * пользователю (допуск по ЕГЭ или БВИ), а место БВИ — в плане и с подтверждённым БВИ.
	 */
	private static void validate(PlanCompositionDto composition, Evaluation evaluation, boolean requireAvailable) {
		List<PlanUniversityDto> universities = composition.universities();
		if (universities.size() > ApplicationPlanGenerator.MAX_UNIVERSITIES) {
			throw new BadRequestException("В плане может быть не больше 5 вузов");
		}
		Set<String> seenUniversities = new HashSet<>();
		Set<String> seenPrograms = new HashSet<>();
		for (PlanUniversityDto university : universities) {
			if (!seenUniversities.add(university.universityId())) {
				throw new BadRequestException("Вузы в плане не должны повторяться");
			}
			if (university.programIds().isEmpty()
					|| university.programIds().size() > ApplicationPlanGenerator.MAX_PROGRAMS) {
				throw new BadRequestException("В каждом вузе должно быть от 1 до 5 программ");
			}
			Set<String> directions = new HashSet<>();
			for (String programId : university.programIds()) {
				Program program = evaluation.programs().get(programId);
				if (program == null) {
					throw new BadRequestException("Программа «" + programId + "» не найдена");
				}
				if (!program.getUniversity().getId().equals(university.universityId())) {
					throw new BadRequestException("Программа «" + program.getName() + "» не относится к вузу «"
							+ university.universityId() + "»");
				}
				if (!seenPrograms.add(programId)) {
					throw new BadRequestException("Программа «" + program.getName() + "» указана дважды");
				}
				if (!directions.add(program.getDirectionId())) {
					throw new BadRequestException("В вузе «" + program.getUniversity().getShortName()
							+ "» уже есть программа этого направления");
				}
				if (requireAvailable && !available(evaluation.options().get(programId))) {
					throw new BadRequestException(label(program) + ": нет допуска к конкурсу, не хватает ЕГЭ "
							+ "или балл ниже минимального");
				}
			}
		}
		String bviProgramId = composition.bviProgramId();
		if (requireAvailable && bviProgramId != null) {
			if (!seenPrograms.contains(bviProgramId)) {
				throw new BadRequestException("Программа, где используется БВИ, должна быть в плане");
			}
			if (!evaluation.options().get(bviProgramId).bviAvailable()) {
				throw new BadRequestException(label(evaluation.programs().get(bviProgramId)) + ": БВИ недоступно");
			}
		}
	}

	private static boolean available(ProgramOptionResponse option) {
		return option.bviAvailable() || "eligible".equals(option.eligibility());
	}

	/** Участвует ли программа в автоплане: БВИ или допуск и отставание не больше допустимого. */
	static boolean eligibleForPlan(ProgramOptionResponse option, int deficit) {
		if (option.bviAvailable()) {
			return true;
		}
		return "eligible".equals(option.eligibility())
				&& (option.scoreDifference() == null || option.scoreDifference() >= -deficit);
	}

	/** Прошлогодний проходной / максимум суммы испытаний, чтобы шкалы с разным числом экзаменов были сравнимы. */
	static Double relativePassingScore(Program program) {
		Integer passing = program.getPassingScorePreviousYear();
		if (passing == null) {
			return null;
		}
		long exams = program.getSubjects().stream()
				.map(subject -> subject.getChoiceGroup() == null
						? "subject:" + subject.getSubjectId()
						: "group:" + subject.getChoiceGroup())
				.distinct()
				.count();
		return exams == 0 ? null : passing / (exams * 100.0);
	}

	private static Candidate candidate(Program program, ProgramOptionResponse option, int deficit) {
		return new Candidate(program.getId(), program.getUniversity().getId(), program.getDirectionId(),
				option.bviAvailable(), eligibleForPlan(option, deficit), relativePassingScore(program));
	}

	/** Невыбранные направления, у которых есть общая категория интересов с выбранными. */
	private static Set<String> related(List<String> selected, Map<String, StudyDirection> directions) {
		Set<String> categories = selected.stream()
				.map(directions::get)
				.filter(Objects::nonNull)
				.flatMap(direction -> direction.getInterestCategoryIds().stream())
				.collect(Collectors.toSet());
		return directions.values().stream()
				.filter(direction -> !selected.contains(direction.getId()))
				.filter(direction -> direction.getInterestCategoryIds().stream().anyMatch(categories::contains))
				.map(StudyDirection::getId)
				.collect(Collectors.toSet());
	}

	/** В режиме «дополнить» выбор БВИ из базового плана сохраняется, если он ещё в плане и действителен. */
	private static String keptBvi(String baseBvi, Set<String> inPlan, Evaluation evaluation, List<String> warnings) {
		if (baseBvi == null) {
			return null;
		}
		ProgramOptionResponse option = evaluation.options().get(baseBvi);
		if (inPlan.contains(baseBvi) && option != null && option.bviAvailable()) {
			return baseBvi;
		}
		warnings.add("Выбор места для БВИ снят: для этой программы БВИ сейчас недоступно.");
		return null;
	}

	private List<String> previewWarnings(Result result, PlanCompositionDto composition, Evaluation evaluation,
			Map<String, StudyDirection> directions, int deficit, PlanCompositionDto base) {
		List<String> warnings = new ArrayList<>();
		for (String directionId : result.directionsWithoutCandidates()) {
			StudyDirection direction = directions.get(directionId);
			warnings.add("По направлению «" + (direction == null ? directionId : direction.getName())
					+ "» нет подходящих программ (допустимое отставание от прошлогоднего проходного — "
					+ deficit + " баллов).");
		}
		if (!result.bviOutsidePlan().isEmpty()) {
			warnings.add("БВИ доступно вне плана: " + result.bviOutsidePlan().stream()
					.limit(3)
					.map(id -> label(evaluation.programs().get(id)))
					.collect(Collectors.joining(", "))
					+ ". Автоплан не назначает БВИ, решение за вами.");
		}
		boolean bviInPlan = composition.universities().stream()
				.flatMap(university -> university.programIds().stream())
				.anyMatch(id -> evaluation.options().get(id).bviAvailable());
		if (bviInPlan && composition.bviProgramId() == null) {
			warnings.add("В плане есть программы с доступным БВИ. Отметьте одну, где хотите его использовать.");
		}
		if (composition.universities().size() < ApplicationPlanGenerator.MAX_UNIVERSITIES) {
			warnings.add("Подходящих вузов: " + composition.universities().size()
					+ " из 5. Неподходящие варианты автоплан не добавляет.");
		}
		String shortUniversities = composition.universities().stream()
				.filter(university -> university.programIds().size() < ApplicationPlanGenerator.MAX_PROGRAMS)
				.map(university -> universityName(university.universityId(), evaluation) + " — "
						+ university.programIds().size())
				.collect(Collectors.joining(", "));
		if (!shortUniversities.isEmpty()) {
			warnings.add("Меньше пяти подходящих направлений: " + shortUniversities + ".");
		}
		base.universities().stream()
				.flatMap(university -> university.programIds().stream())
				.filter(id -> !available(evaluation.options().get(id)))
				.forEach(id -> warnings.add(label(evaluation.programs().get(id))
						+ " из вашего плана сейчас не проходит по условиям, но оставлена."));
		return warnings;
	}

	private static List<PlanExplanationResponse> explanations(Result result, Evaluation evaluation,
			Map<String, StudyDirection> directions) {
		return result.universities().stream()
				.flatMap(university -> university.programs().stream())
				.map(choice -> {
					Program program = evaluation.programs().get(choice.programId());
					String message = switch (choice.reason()) {
						case SELECTED_DIRECTION -> "Выбранное направление «" + directionName(program, directions) + "»";
						case RELATED_DIRECTION -> "Смежное направление: общая категория интересов с выбранными";
						case AUTO_FILL -> "Добор подходящего направления";
						case RETAINED -> "Оставлено из вашего плана";
					};
					return new PlanExplanationResponse(choice.programId(), reasonCode(choice.reason()), message);
				})
				.toList();
	}

	private static String reasonCode(ApplicationPlanGenerator.Reason reason) {
		return switch (reason) {
			case SELECTED_DIRECTION -> "selected_direction";
			case RELATED_DIRECTION -> "related_direction";
			case AUTO_FILL -> "auto_fill";
			case RETAINED -> "retained";
		};
	}

	private static List<PlanUniversityRankingResponse> ranking(Result result, Evaluation evaluation,
			Map<String, StudyDirection> directions) {
		return result.universities().stream()
				.map(university -> {
					List<PlanTopPlaceResponse> places = university.places().stream()
							.map(place -> new PlanTopPlaceResponse(place.directionId(),
									directions.containsKey(place.directionId())
											? directions.get(place.directionId()).getName()
											: place.directionId(),
									place.place()))
							.toList();
					return new PlanUniversityRankingResponse(university.universityId(),
							universityName(university.universityId(), evaluation), university.hits(),
							university.sumOfPlaces(), university.firstPlaces(), places, university.addedAsFill(),
							university.retained(), rankingMessage(university, places));
				})
				.toList();
	}

	private static String rankingMessage(UniversityChoice university, List<PlanTopPlaceResponse> places) {
		if (places.isEmpty()) {
			return university.retained()
					? "Вуз из вашего плана"
					: "Добавлен при доборе: в топ-3 по выбранным направлениям не вошёл";
		}
		String topPlaces = places.stream()
				.map(place -> "«" + place.directionName() + "» — " + place.place() + " место")
				.collect(Collectors.joining("; "));
		return (university.retained() ? "Вуз из вашего плана. " : "") + "Топ-3 по направлениям: " + topPlaces
				+ ". Попаданий: " + university.hits() + ", сумма мест: " + university.sumOfPlaces()
				+ ", первых мест: " + university.firstPlaces() + ".";
	}

	private ApplicationPlanResponse toResponse(PlanCompositionDto composition, Evaluation evaluation, int version,
			OffsetDateTime savedAt) {
		List<String> warnings = new ArrayList<>();
		composition.universities().stream()
				.flatMap(university -> university.programIds().stream())
				.filter(id -> evaluation.options().containsKey(id) && !available(evaluation.options().get(id)))
				.forEach(id -> warnings.add(label(evaluation.programs().get(id))
						+ ": по текущим данным нет допуска к конкурсу."));
		String bvi = composition.bviProgramId();
		if (bvi != null && evaluation.options().containsKey(bvi) && !evaluation.options().get(bvi).bviAvailable()) {
			warnings.add("БВИ для " + label(evaluation.programs().get(bvi)) + " больше недоступно. Снимите выбор.");
		}
		return new ApplicationPlanResponse(composition, options(composition, evaluation), List.of(), List.of(),
				warnings, OffsetDateTime.now(ZoneOffset.UTC), nearPreviousThreshold, version, savedAt);
	}

	private static List<ProgramOptionResponse> options(PlanCompositionDto composition, Evaluation evaluation) {
		return composition.universities().stream()
				.flatMap(university -> university.programIds().stream())
				.map(evaluation.options()::get)
				.filter(Objects::nonNull)
				.toList();
	}

	private static PlanCompositionDto toComposition(ApplicationPlan plan) {
		Map<String, List<ApplicationPlanItem>> byUniversity = plan.getItems().stream()
				.sorted(Comparator.comparingInt(ApplicationPlanItem::getUniversityPosition)
						.thenComparingInt(ApplicationPlanItem::getProgramPosition))
				.collect(Collectors.groupingBy(ApplicationPlanItem::getUniversityId, LinkedHashMap::new,
						Collectors.toList()));
		List<PlanUniversityDto> universities = byUniversity.entrySet().stream()
				.map(entry -> new PlanUniversityDto(entry.getKey(),
						entry.getValue().stream().map(ApplicationPlanItem::getProgramId).toList()))
				.toList();
		return new PlanCompositionDto(universities, plan.getBviProgramId());
	}

	private Evaluation evaluate(UUID sessionId) {
		List<Program> programs = programRepository.findAllByOrderByUniversityIdAscIdAsc();
		List<ProgramOptionResponse> options = recommendationService.evaluatePrograms(sessionId, programs);
		Map<String, Program> programsById = new LinkedHashMap<>();
		Map<String, ProgramOptionResponse> optionsById = new LinkedHashMap<>();
		for (int index = 0; index < programs.size(); index++) {
			programsById.put(programs.get(index).getId(), programs.get(index));
			optionsById.put(programs.get(index).getId(), options.get(index));
		}
		return new Evaluation(programsById, optionsById);
	}

	private static String label(Program program) {
		return "«" + program.getName() + "» (" + program.getUniversity().getShortName() + ")";
	}

	private static String universityName(String universityId, Evaluation evaluation) {
		return evaluation.programs().values().stream()
				.filter(program -> program.getUniversity().getId().equals(universityId))
				.map(program -> program.getUniversity().getShortName())
				.findFirst()
				.orElse(universityId);
	}

	private static String directionName(Program program, Map<String, StudyDirection> directions) {
		StudyDirection direction = directions.get(program.getDirectionId());
		return direction == null ? program.getDirectionId() : direction.getName();
	}

	private User getUser(UUID sessionId) {
		return userRepository.findById(sessionId)
				.orElseThrow(() -> new ResourceNotFoundException("Заявка не найдена"));
	}

	/** Все программы и их персональный расчёт, по id. */
	private record Evaluation(Map<String, Program> programs, Map<String, ProgramOptionResponse> options) {
	}
}
