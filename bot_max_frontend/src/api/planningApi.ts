import { request } from "@/api/client";
import type {
  ApplicationPlan,
  DirectionSelection,
  Diploma,
  SavedDiploma,
  Olympiad,
  Page,
  PlanComposition,
  PlanPreview,
  PlanPreviewRequest,
  ProgramOption,
  StudyDirection,
} from "@/api/types/planning";

const sessionPath = (id: string) => `/sessions/${encodeURIComponent(id)}`;
export function getDirections(
  query = "",
  offset = 0,
  interestCategoryId = "",
): Promise<Page<StudyDirection>> {
  const params = new URLSearchParams({
    query,
    offset: String(offset),
    limit: "20",
  });
  if (interestCategoryId) params.set("interestCategoryId", interestCategoryId);
  return request(`/directions?${params}`);
}
export function getSelectedDirections(id: string): Promise<DirectionSelection> {
  return request(`${sessionPath(id)}/directions`);
}
export function saveSelectedDirections(
  id: string,
  directionIds: string[],
): Promise<DirectionSelection> {
  return request(`${sessionPath(id)}/directions`, {
    method: "PUT",
    body: JSON.stringify({ directionIds }),
  });
}
export function getRecommendations(
  id: string,
  directionId: string,
  offset = 0,
): Promise<Page<ProgramOption>> {
  return request(
    `${sessionPath(id)}/recommendations?${new URLSearchParams({ directionId, offset: String(offset), limit: "20" })}`,
  );
}
export function getApplicationPlan(id: string): Promise<ApplicationPlan> {
  return request(`${sessionPath(id)}/application-plan`);
}
export function saveApplicationPlan(
  id: string,
  composition: PlanComposition,
  expectedVersion: number,
): Promise<ApplicationPlan> {
  return request(`${sessionPath(id)}/application-plan`, {
    method: "PUT",
    body: JSON.stringify({ ...composition, expectedVersion }),
  });
}
/** Автоплан 5×5: только черновик, для сохранения нужен saveApplicationPlan. */
export function previewApplicationPlan(
  id: string,
  body: PlanPreviewRequest,
): Promise<PlanPreview> {
  return request(`${sessionPath(id)}/application-plan/preview`, {
    method: "POST",
    body: JSON.stringify(body),
  });
}
export function getOlympiads(): Promise<Olympiad[]> {
  return request("/olympiads");
}
export function getDiplomas(id: string): Promise<SavedDiploma[]> {
  return request(`${sessionPath(id)}/olympiads`);
}
export function saveDiplomas(
  id: string,
  diplomas: Diploma[],
): Promise<SavedDiploma[]> {
  return request(`${sessionPath(id)}/olympiads`, {
    method: "PUT",
    body: JSON.stringify({
      diplomas: diplomas.map(({ profileId, degree }) => ({
        profileId,
        degree,
      })),
    }),
  });
}
