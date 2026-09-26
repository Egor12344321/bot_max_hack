import type { RiskStatus } from "@/api/types/universities";

export interface PriorityListItem {
  universityId: string;
  universityName: string;

  priority: number;

  chancePercent: number;

  myScore: number;
  passingScorePreviousYear: number;

  status: RiskStatus;
}

export interface SetPrioritiesRequest {
  universityIds: string[];
}
