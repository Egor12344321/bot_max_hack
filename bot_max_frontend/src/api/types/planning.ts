export interface StudyDirection {
  id: string;
  code: string;
  name: string;
}
export interface Page<T> {
  items: T[];
  total: number;
}
export interface DirectionSelection {
  directionIds: string[];
}
export interface ProgramOption {
  programId: string;
  programName: string;
  universityId: string;
  universityName: string;
  direction: StudyDirection;
  city: string;
  campus: string | null;
  campaignYear: number;
  funding: "budget" | "paid";
  studyForm: "full_time" | "part_time" | "correspondence";
  competitionType:
    | "general"
    | "special_quota"
    | "separate_quota"
    | "target_quota";
  eligibility: "eligible" | "incomplete" | "ineligible";
  bviAvailable: boolean;
  totalScore: number | null;
  passingScorePreviousYear: number | null;
  previousYear: number | null;
  scoreDifference: number | null;
  comparison:
    | "bvi"
    | "above_previous"
    | "near_previous"
    | "below_previous"
    | "insufficient_data"
    | "ineligible";
  dataSource: "demo" | "verified";
  breakdown: {
    kind: "exam" | "olympiad" | "achievement";
    name: string;
    originalScore: number | null;
    countedScore: number | null;
    applied: boolean;
    explanation: string;
  }[];
  reasons: string[];
}
export interface PlanComposition {
  universities: { universityId: string; programIds: string[] }[];
  bviProgramId: string | null;
}
export interface ApplicationPlan {
  composition: PlanComposition;
  options: ProgramOption[];
  version: number;
  savedAt: string | null;
  warnings: string[];
  calculatedAt: string;
  nearPreviousThreshold: number;
  explanations: PlanExplanation[];
  universityRanking?: PlanUniversityRanking[];
}
export interface PlanExplanation {
  programId: string;
  reason: "selected_direction" | "related_direction" | "auto_fill" | "retained";
  message: string;
}
/** Почему вуз в автоплане: места в топ-3 по направлениям и сводные показатели. */
export interface PlanUniversityRanking {
  universityId: string;
  universityName: string;
  hits: number;
  sumOfPlaces: number;
  firstPlaces: number;
  places: { directionId: string; directionName: string; place: number }[];
  addedAsFill: boolean;
  retained: boolean;
  message: string;
}
/** Допустимое отставание от прошлогоднего проходного в автоплане. */
export type AllowedDeficit = 0 | 10 | 15 | 20;
export type PlanPreviewRequest =
  | { mode: "generate"; allowedDeficit?: AllowedDeficit }
  | { mode: "fill"; basePlan: PlanComposition; allowedDeficit?: AllowedDeficit };
/** Черновик автоплана: сервер ничего не сохраняет. */
export interface PlanPreview {
  composition: PlanComposition;
  options: ProgramOption[];
  explanations: PlanExplanation[];
  universityRanking: PlanUniversityRanking[];
  warnings: string[];
  calculatedAt: string;
  nearPreviousThreshold: number;
}
export interface Diploma {
  profileId: string;
  degree: "winner" | "prize";
}
export interface SavedDiploma extends Diploma {
  olympiadId: string;
  olympiadName: string;
  profileName: string;
  level: number | null;
  year: number;
}
export interface Olympiad {
  id: string;
  name: string;
  isVsosh: boolean;
  listNumber: number | null;
  profiles: {
    id: string;
    profile: string;
    name: string;
    subjectId: string;
    level: number | null;
    year: number;
  }[];
}
