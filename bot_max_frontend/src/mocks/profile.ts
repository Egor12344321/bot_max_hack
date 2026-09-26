import type { ProfileSummary } from "@/api/types/profile";

export const mockProfile: ProfileSummary = {
  fullName: "Алексей Смирнов",
  graduationYear: 2027,

  mainInterestCategory: "IT",

  totalScore: 285,
  maxScore: 310,
  achievementsBonus: 10,

  admissionProbabilityPercent: 78,
  scoreDeltaVsAveragePassing: 9,

  reserveCount: 2,
  realCount: 3,
  riskCount: 1,

  advice:
    "Поставь один из вузов с высоким шансом поступления выше в списке приоритетов.",
};
