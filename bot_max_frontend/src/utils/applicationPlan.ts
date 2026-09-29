import type { PlanComposition, ProgramOption } from "@/api/types/planning";
export const emptyComposition = (): PlanComposition => ({
  universities: [],
  bviProgramId: null,
});
export function moveItem<T>(items: T[], index: number, delta: number): T[] {
  const next = [...items];
  const target = index + delta;
  if (
    index < 0 ||
    index >= items.length ||
    target < 0 ||
    target >= items.length
  )
    return next;
  [next[index], next[target]] = [next[target], next[index]];
  return next;
}
export function validateComposition(
  plan: PlanComposition,
  options: ProgramOption[],
): string | null {
  if (plan.universities.length > 5)
    return "В плане может быть не больше 5 вузов.";
  const universities = new Set<string>();
  const programs = new Map(options.map((item) => [item.programId, item]));
  const years = new Set<number>();
  const selected = new Set<string>();
  for (const university of plan.universities) {
    if (universities.has(university.universityId))
      return "Вузы не должны повторяться.";
    universities.add(university.universityId);
    if (!university.programIds.length || university.programIds.length > 5)
      return "В каждом вузе должно быть от 1 до 5 направлений.";
    const directions = new Set<string>();
    for (const id of university.programIds) {
      const option = programs.get(id);
      if (!option || option.universityId !== university.universityId)
        return "Не удалось проверить программу. Обновите данные.";
      if (directions.has(option.direction.id))
        return "В вузе уже выбрана программа этого направления. Сначала удалите её для замены.";
      directions.add(option.direction.id);
      years.add(option.campaignYear);
      selected.add(id);
    }
  }
  if (years.size > 1) return "Выберите программы одной приёмной кампании.";
  if (
    plan.bviProgramId &&
    (!selected.has(plan.bviProgramId) ||
      !programs.get(plan.bviProgramId)?.bviAvailable)
  )
    return "Выбранное место БВИ недоступно. Снимите выбор или выберите другое.";
  return null;
}
export function addProgram(
  plan: PlanComposition,
  option: ProgramOption,
  options: ProgramOption[],
): PlanComposition {
  if (plan.universities.some((u) => u.programIds.includes(option.programId)))
    return plan;
  const found = plan.universities.some(
    (u) => u.universityId === option.universityId,
  );
  const next = {
    ...plan,
    universities: found
      ? plan.universities.map((u) =>
          u.universityId === option.universityId
            ? { ...u, programIds: [...u.programIds, option.programId] }
            : u,
        )
      : [
          ...plan.universities,
          { universityId: option.universityId, programIds: [option.programId] },
        ],
  };
  const error = validateComposition(next, [...options, option]);
  if (error) throw new Error(error);
  return next;
}
export function removeProgram(
  plan: PlanComposition,
  id: string,
): PlanComposition {
  return {
    bviProgramId: plan.bviProgramId === id ? null : plan.bviProgramId,
    universities: plan.universities
      .map((u) => ({ ...u, programIds: u.programIds.filter((p) => p !== id) }))
      .filter((u) => u.programIds.length),
  };
}
