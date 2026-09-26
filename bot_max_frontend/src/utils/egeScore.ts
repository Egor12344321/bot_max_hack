import type { EgeScoreResult } from "@/api/types/session";

export function getEgeScoreSummary(scores: EgeScoreResult[]) {
  return {
    total: scores.reduce((sum, subject) => sum + subject.score, 0),
    max: scores.length * 100,
  };
}
