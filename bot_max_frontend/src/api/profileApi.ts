import type { Profile } from "@/api/types/profile";

import { request } from "@/api/client";

import { getMockProfile } from "@/mocks/mockApi";

const isMock = import.meta.env.VITE_API_MODE === "mock";

export async function getProfile(sessionId: string): Promise<Profile> {
  if (isMock) {
    return getMockProfile();
  }

  return request<Profile>(`/sessions/${sessionId}/profile`);
}
