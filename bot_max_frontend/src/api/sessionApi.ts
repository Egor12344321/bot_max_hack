import type { SessionDraft } from "@/api/types/session";

import { request } from "@/api/client";

import { getMockSession } from "@/mocks/mockApi";

const isMock = import.meta.env.VITE_API_MODE === "mock";

export async function getSession(sessionId: string): Promise<SessionDraft> {
  if (isMock) {
    return getMockSession();
  }

  return request<SessionDraft>(`/sessions/${sessionId}`);
}
