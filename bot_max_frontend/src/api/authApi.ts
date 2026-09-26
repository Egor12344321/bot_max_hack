import type {
  ExchangeAuthRequest,
  ExchangeAuthResponse,
} from "@/api/types/auth";

import { request } from "@/api/client";

import { exchangeMockAuth } from "@/mocks/mockApi";

const isMock = import.meta.env.VITE_API_MODE === "mock";

export async function exchangeAuth(
  launchParams: string,
): Promise<ExchangeAuthResponse> {
  if (isMock) {
    return exchangeMockAuth();
  }

  const body: ExchangeAuthRequest = {
    launchParams,
  };

  return request<ExchangeAuthResponse>("/auth/exchange", {
    method: "POST",
    body: JSON.stringify(body),
  });
}
