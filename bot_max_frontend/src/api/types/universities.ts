export type RiskStatus = "reserve" | "real" | "risk";

export interface UniversityCard {
  id: string;
  name: string;
  city: string;
  hasDormitory: boolean;
  hasMilitaryDept: boolean;
  directionsCount: number;
}

export interface UniversitySummary {
  id: string;
  name: string;
  priority: number;
  directionsCount: number;

  myScore: number;
  passingScorePreviousYear: number;

  chancePercent: number;
  status: RiskStatus;

  seatsTotal: number | null;
  seatsFilled: number | null;

  tags: string[];
}

export interface AchievementScoreBreakdownItem {
  achievementId: string;
  achievementName: string;
  points: number;
  counted: boolean;
  note: string | null;
}

export interface RiskFactors {
  passingScorePreviousYear: number;

  budgetPlacesPreviousYear: number | null;
  budgetPlacesCurrentYear: number | null;

  competitionIndex: number | null;
}

export interface Direction {
  id: string;
  universityId: string;
  name: string;

  priority: number;

  passingScorePreviousYear: number;
  chancePercent: number;

  riskFactors: RiskFactors;
}

export interface UniversityDetail {
  id: string;
  name: string;

  chancePercent: number;

  idScoreTotal: number;
  idScoreMax: number;

  breakdown: AchievementScoreBreakdownItem[];

  directions: Direction[];
}

export interface AddUniversityRequest {
  universityId: string;
}

export interface SetDirectionPrioritiesRequest {
  directionIds: string[];
}
