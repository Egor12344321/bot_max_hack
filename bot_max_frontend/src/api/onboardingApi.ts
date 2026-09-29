import type {
  Achievement,
  InterestCategory,
  PrivilegeApplyResult,
  PrivilegeCategory,
  SetAchievementsRequest,
  SetInterestsRequest,
  SetPrivilegesRequest,
} from "@/api/types/onboarding";

import { request } from "@/api/client";

import {
  getMockInterests,
  getMockPrivileges,
  saveMockInterests,
  saveMockPrivileges,
} from "@/mocks/mockApi";

const isMock = import.meta.env.VITE_API_MODE === "mock";

export async function getInterests(): Promise<InterestCategory[]> {
  if (isMock) {
    return getMockInterests();
  }

  return request<InterestCategory[]>("/interest-categories");
}

export async function saveInterests(
  sessionId: string,
  categoryIds: string[],
): Promise<void> {
  if (isMock) {
    return saveMockInterests(categoryIds);
  }

  const body: SetInterestsRequest = {
    categoryIds,
  };

  return request<void>(`/sessions/${sessionId}/interests`, {
    method: "PUT",
    body: JSON.stringify(body),
  });
}

export async function getAchievements(): Promise<Achievement[]> {
  return request<Achievement[]>("/achievements");
}

export async function saveAchievements(
  sessionId: string,
  achievementIds: string[],
): Promise<void> {
  const body: SetAchievementsRequest = {
    achievementIds,
  };

  return request<void>(`/sessions/${sessionId}/achievements`, {
    method: "PUT",
    body: JSON.stringify(body),
  });
}

export async function getPrivileges(): Promise<PrivilegeCategory[]> {
  if (isMock) {
    return getMockPrivileges();
  }

  return request<PrivilegeCategory[]>("/privilege-categories");
}

export async function savePrivileges(
  sessionId: string,
  categoryIds: string[],
): Promise<PrivilegeApplyResult> {
  if (isMock) {
    return saveMockPrivileges(categoryIds);
  }

  const body: SetPrivilegesRequest = {
    categoryIds,
  };

  return request<PrivilegeApplyResult>(`/sessions/${sessionId}/privilege`, {
    method: "PUT",
    body: JSON.stringify(body),
  });
}
