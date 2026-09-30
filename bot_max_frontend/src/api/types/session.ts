
export interface Session {
  id: string;
  platform: string;
  countryCode: string | null;
  createdAt: string;
}

export type CitizenshipGroup = "eaeu" | "other";

export interface CitizenshipOption {
  countryCode: string;
  name: string;
  group: CitizenshipGroup;
}

export type CitizenshipTrack = "domestic_equivalent" | "rf_quota_or_paid";

export interface CitizenshipLink {
  label: string;
  url: string;
}

export interface CitizenshipResult {
  track: CitizenshipTrack;
  message: string;
  links: CitizenshipLink[];
}

export interface Subject {
  id: string;
  name: string;
  minThreshold: number;
}

export interface EgeScoreInput {
  subjectId: string;
  score: number;
}

export interface EgeScoreResult {
  subjectId: string;
  subjectName: string;
  score: number;
  minThreshold: number;
  passed: boolean;
}

export interface EgeScoresSubmission {
  scores: EgeScoreResult[];
  allPassed: boolean;
}

export interface SessionDraft {
  id: string;
  countryCode: string | null;
  egeScores: EgeScoreResult[];
  isCompleteFromBot: boolean;
}
