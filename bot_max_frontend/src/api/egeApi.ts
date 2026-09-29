import type { EgeScoreInput, EgeScoresSubmission, Subject } from "@/api/types/session";

import { request } from "@/api/client";

import { getMockEgeScores, getMockSubjects, saveMockEgeScores } from "@/mocks/mockApi";

const isMock = import.meta.env.VITE_API_MODE === "mock";

export async function getSubjects(): Promise<Subject[]> {
  if (isMock) {
    return getMockSubjects();
  }

  return request<Subject[]>("/subjects");
}

export async function getEgeScores(sessionId: string): Promise<EgeScoresSubmission> {
  if (isMock) {
    return getMockEgeScores();
  }

  return request<EgeScoresSubmission>(`/sessions/${sessionId}/ege-scores`);
}

/** Полностью заменяет баллы ЕГЭ пользователя. */
export async function saveEgeScores(
  sessionId: string,
  scores: EgeScoreInput[],
): Promise<EgeScoresSubmission> {
  if (isMock) {
    return saveMockEgeScores(scores);
  }

  return request<EgeScoresSubmission>(`/sessions/${sessionId}/ege-scores`, {
    method: "PUT",
    body: JSON.stringify({ scores }),
  });
}
