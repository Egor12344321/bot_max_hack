import type {
  ApplicationPlan,
  PlanComposition,
  ProgramOption,
} from "@/api/types/planning";
import { validateComposition } from "@/utils/applicationPlan";

export interface PlanDraft {
  composition: PlanComposition;
  options: ProgramOption[];
  expectedVersion: number;
}
const key = (sessionId: string) => `application-plan-draft:v1:${sessionId}`;
export function readPlanDraft(sessionId: string): PlanDraft | null {
  try {
    const draft = JSON.parse(
      sessionStorage.getItem(key(sessionId)) ?? "null",
    ) as PlanDraft | null;
    if (
      !draft ||
      !Number.isInteger(draft.expectedVersion) ||
      draft.expectedVersion < 0 ||
      !Array.isArray(draft.options)
    )
      return null;
    if (
      validateComposition(
        { ...draft.composition, bviProgramId: null },
        draft.options,
      )
    )
      return null;
    return draft;
  } catch {
    return null;
  }
}
export function writePlanDraft(sessionId: string, draft: PlanDraft): boolean {
  try {
    sessionStorage.setItem(key(sessionId), JSON.stringify(draft));
    return true;
  } catch {
    return false;
  }
}
export function clearPlanDraft(sessionId: string) {
  try {
    sessionStorage.removeItem(key(sessionId));
  } catch {
    /* The in-memory saved plan remains usable. */
  }
}
export function planToDraft(plan: ApplicationPlan): PlanDraft {
  return {
    composition: plan.composition,
    options: plan.options,
    expectedVersion: plan.version,
  };
}
