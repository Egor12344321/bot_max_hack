import type { PriorityListItem } from "@/api/types/priorities";
import { mockUniversities } from "@/mocks/universities";

export const mockPriorities: PriorityListItem[] = mockUniversities.map(
  (university) => ({
    universityId: university.id,
    universityName: university.name,
    priority: university.priority,
    chancePercent: university.chancePercent,
    myScore: university.myScore,
    passingScorePreviousYear: university.passingScorePreviousYear,
    status: university.status,
  }),
);
