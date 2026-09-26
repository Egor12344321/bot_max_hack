export interface ProfileSummary {
  fullName: string;
  graduationYear: number;

  mainInterestCategory: string;

  totalScore: number;
  maxScore: number;
  achievementsBonus: number;

  admissionProbabilityPercent: number;
  scoreDeltaVsAveragePassing: number;

  reserveCount: number;
  realCount: number;
  riskCount: number;

  advice: string;
}
