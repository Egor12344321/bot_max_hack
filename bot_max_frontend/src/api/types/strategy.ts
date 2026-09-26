import type { PriorityListItem } from "@/api/types/priorities";

export interface StrategyFinalizeResponse {
  status: string;

  summary: PriorityListItem[];

  reportUrl: string | null;
}

export type StrategyReportFormat = "pdf" | "json";
