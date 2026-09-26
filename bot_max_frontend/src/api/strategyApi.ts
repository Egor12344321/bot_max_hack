import type { StrategyFinalizeResponse } from "@/api/types/strategy";

import { request } from "@/api/client";

import { finalizeMockStrategy, getMockStrategyReport } from "@/mocks/mockApi";

const isMock = import.meta.env.VITE_API_MODE === "mock";

export async function finalizeStrategy(
  sessionId: string,
): Promise<StrategyFinalizeResponse> {
  if (isMock) {
    return finalizeMockStrategy();
  }

  return request<StrategyFinalizeResponse>(`/sessions/${sessionId}/strategy`, {
    method: "POST",
  });
}

export async function getStrategyReport(
  sessionId: string,
): Promise<StrategyFinalizeResponse> {
  if (isMock) {
    return getMockStrategyReport();
  }

  return request<StrategyFinalizeResponse>(
    `/sessions/${sessionId}/strategy/report?format=json`,
  );
}
