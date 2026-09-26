import type { Session } from "@/api/types/session";

export interface ExchangeAuthRequest {
  launchParams: string;
}

export interface ExchangeAuthResponse {
  accessToken: string;
  expiresIn: number;
  session: Session;
}
