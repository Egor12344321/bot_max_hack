import type {
  PriorityListItem,
  SetPrioritiesRequest,
} from "@/api/types/priorities";

import { request } from "@/api/client";

import { getMockPriorities, saveMockPriorities } from "@/mocks/mockApi";

const isMock = import.meta.env.VITE_API_MODE === "mock";

export async function getPriorities(
  sessionId: string,
): Promise<PriorityListItem[]> {
  if (isMock) {
    return getMockPriorities();
  }

  return request<PriorityListItem[]>(`/sessions/${sessionId}/priorities`);
}

export async function savePriorities(
  sessionId: string,
  universityIds: string[],
): Promise<PriorityListItem[]> {
  if (isMock) {
    return saveMockPriorities(universityIds);
  }

  const body: SetPrioritiesRequest = {
    universityIds,
  };

  return request<PriorityListItem[]>(`/sessions/${sessionId}/priorities`, {
    method: "PUT",
    body: JSON.stringify(body),
  });
}
