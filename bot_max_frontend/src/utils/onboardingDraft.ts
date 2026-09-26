import { getMockStorage } from "@/mocks/mockStorage";

export type SelectionField = "selectedInterestIds" | "selectedAchievementIds" | "selectedPrivilegeIds";

function draftKey(sessionId: string, field: SelectionField) {
  return `onboarding:${import.meta.env.VITE_API_MODE}:${sessionId}:${field}`;
}

export function readOnboardingSelection(sessionId: string, field: SelectionField): string[] {
  try {
    const saved: unknown = JSON.parse(localStorage.getItem(draftKey(sessionId, field)) ?? "null");
    if (Array.isArray(saved) && saved.every((id) => typeof id === "string")) return saved;
  } catch { /* Ignore an invalid or unavailable browser cache. */ }
  if (import.meta.env.VITE_API_MODE === "mock") {
    try { return [...getMockStorage()[field]]; } catch { /* Use an empty draft. */ }
  }
  return [];
}

export function writeOnboardingSelection(sessionId: string, field: SelectionField, ids: string[]) {
  try {
    localStorage.setItem(draftKey(sessionId, field), JSON.stringify(ids));
  } catch { /* Keep the in-memory selection usable without persistence. */ }
}
