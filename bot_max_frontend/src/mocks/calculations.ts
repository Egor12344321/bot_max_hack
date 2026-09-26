import type { RiskStatus, UniversityDetail, UniversitySummary } from "@/api/types/universities";
import type { MockStorageData } from "@/mocks/mockStorage";
import { mockAchievements } from "@/mocks/achievements";
import { mockPrivileges } from "@/mocks/privileges";
import { mockSession } from "@/mocks/session";
import { mockUniversities, mockUniversityDetails } from "@/mocks/universities";
import { getEgeScoreSummary } from "@/utils/egeScore";

// Demo heuristic only. Real admission probabilities must come from the API.
function chance(base: number, scoreDelta: number, storage: MockStorageData) {
  const hasBvi = mockPrivileges.some((item) =>
    item.quotaType === "bvi" && storage.selectedPrivilegeIds.includes(item.id));
  return hasBvi ? 100 : Math.max(0, Math.min(100, base + scoreDelta * 2));
}

function riskStatus(percent: number): RiskStatus {
  return percent >= 80 ? "reserve" : percent >= 50 ? "real" : "risk";
}

export function calculateMockDetail(id: string, storage: MockStorageData): UniversityDetail {
  const base = mockUniversityDetails.find((item) => item.id === id);
  if (!base) throw new Error("University not found");
  let remaining = base.idScoreMax;
  const breakdown = mockAchievements
    .filter((item) => storage.selectedAchievementIds.includes(item.id))
    .map((achievement) => {
      const rule = base.breakdown.find((item) => item.achievementId === achievement.id);
      const points = Math.min(remaining, rule?.counted ? rule.points : 0);
      remaining -= points;
      return {
        achievementId: achievement.id,
        achievementName: achievement.name,
        points,
        counted: points > 0,
        note: points === 0 ? "Не учитывается в этом вузе" : points < (rule?.points ?? 0) ? "Достигнут максимум баллов за ИД" : null,
      };
    });
  const idScoreTotal = base.idScoreMax - remaining;
  const order = storage.directionPriorityIds[id] ?? [];
  const orderedIds = [...new Set([...order, ...base.directions.map((item) => item.id)])];
  const directions = orderedIds.flatMap((directionId) => {
    const direction = base.directions.find((item) => item.id === directionId);
    return direction ? [{
      ...direction,
      chancePercent: chance(direction.chancePercent, idScoreTotal - base.idScoreTotal, storage),
    }] : [];
  }).map((direction, index) => ({ ...direction, priority: index + 1 }));
  return {
    ...base,
    breakdown,
    idScoreTotal,
    directions,
    chancePercent: directions[0]?.chancePercent ?? chance(base.chancePercent, idScoreTotal - base.idScoreTotal, storage),
  };
}

export function calculateMockUniversities(storage: MockStorageData): UniversitySummary[] {
  const ids = [...new Set([...storage.universityPriorityIds, ...storage.universityIds])]
    .filter((id) => storage.universityIds.includes(id));
  return ids.flatMap((id) => {
    const base = mockUniversities.find((item) => item.id === id);
    if (!base) return [];
    const detail = calculateMockDetail(id, storage);
    return [{
      ...base,
      directionsCount: detail.directions.length,
      myScore: getEgeScoreSummary(mockSession.egeScores).total + detail.idScoreTotal,
      chancePercent: detail.chancePercent,
      status: riskStatus(detail.chancePercent),
      passingScorePreviousYear: detail.directions[0]?.passingScorePreviousYear ?? base.passingScorePreviousYear,
    }];
  }).map((university, index) => ({ ...university, priority: index + 1 }));
}
