import type {
  AddUniversityRequest,
  Direction,
  SetDirectionPrioritiesRequest,
  UniversityCard,
  UniversityDetail,
  UniversitySummary,
} from "@/api/types/universities";

import { request } from "@/api/client";

import {
  addMockUniversity,
  getMockUniversities,
  getMockUniversityDetail,
  removeMockUniversity,
  saveMockDirectionPriorities,
  searchMockUniversities,
} from "@/mocks/mockApi";

const isMock = import.meta.env.VITE_API_MODE === "mock";

export async function getUniversities(
  sessionId: string,
): Promise<UniversitySummary[]> {
  if (isMock) {
    return getMockUniversities();
  }

  return request<UniversitySummary[]>(`/sessions/${sessionId}/universities`);
}

export async function searchUniversities(
  query: string,
): Promise<UniversityCard[]> {
  if (isMock) {
    return searchMockUniversities(query);
  }

  const params = new URLSearchParams();

  params.set("query", query);

  return request<UniversityCard[]>(`/universities/search?${params.toString()}`);
}

export async function getUniversityDetail(
  sessionId: string,
  universityId: string,
): Promise<UniversityDetail> {
  if (isMock) {
    return getMockUniversityDetail(universityId);
  }

  return request<UniversityDetail>(
    `/sessions/${sessionId}/universities/${universityId}`,
  );
}

export async function addUniversity(
  sessionId: string,
  universityId: string,
): Promise<UniversitySummary> {
  if (isMock) {
    return addMockUniversity(universityId);
  }

  const body: AddUniversityRequest = {
    universityId,
  };

  return request<UniversitySummary>(`/sessions/${sessionId}/universities`, {
    method: "POST",
    body: JSON.stringify(body),
  });
}

export async function removeUniversity(
  sessionId: string,
  universityId: string,
): Promise<void> {
  if (isMock) {
    return removeMockUniversity(universityId);
  }

  return request<void>(`/sessions/${sessionId}/universities/${universityId}`, {
    method: "DELETE",
  });
}

export async function saveDirectionPriorities(
  sessionId: string,
  universityId: string,
  directionIds: string[],
): Promise<Direction[]> {
  if (isMock) {
    const university = await saveMockDirectionPriorities(
      universityId,
      directionIds,
    );

    return university.directions;
  }

  const body: SetDirectionPrioritiesRequest = {
    directionIds,
  };

  return request<Direction[]>(
    `/sessions/${sessionId}/universities/${universityId}/directions/priorities`,
    {
      method: "PUT",
      body: JSON.stringify(body),
    },
  );
}
