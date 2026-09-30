import type {
  Achievement,
  InterestCategory,
  PrivilegeApplyResult,
  PrivilegeCategory,
} from "@/api/types/onboarding";

import type { Profile } from "@/api/types/profile";

import type { ExchangeAuthResponse } from "@/api/types/auth";


import type { PriorityListItem } from "@/api/types/priorities";

import type { EgeScoreInput, EgeScoresSubmission, SessionDraft, Subject } from "@/api/types/session";

import type {
  UniversityCard,
  UniversityDetail,
  UniversitySummary,
} from "@/api/types/universities";

import { mockSession } from "@/mocks/session";
import { mockInterests } from "@/mocks/interests";
import { mockAchievements } from "@/mocks/achievements";
import { mockPrivileges } from "@/mocks/privileges";
import { mockProfile, mockSubjects } from "@/mocks/profile";

import {
  mockUniversities,
  mockUniversityCards,
  mockUniversityDetails,
} from "@/mocks/universities";

import { finalizeMockStrategy, getMockStrategyReport } from "@/mocks/strategy";


import { getMockStorage, saveMockStorage } from "@/mocks/mockStorage";
import { calculateMockDetail, calculateMockUniversities } from "@/mocks/calculations";
import { getEgeScoreSummary } from "@/utils/egeScore";

function delay(ms = 300) {
  return new Promise((resolve) => {
    setTimeout(resolve, ms);
  });
}

export async function getMockSession(): Promise<SessionDraft> {
  await delay();

  return mockSession;
}

export async function getMockInterests(): Promise<InterestCategory[]> {
  await delay();

  return mockInterests;
}

export async function saveMockInterests(categoryIds: string[]): Promise<void> {
  await delay();

  const storage = getMockStorage();

  storage.selectedInterestIds = categoryIds;

  saveMockStorage(storage);
}

export async function getMockAchievements(): Promise<Achievement[]> {
  await delay();

  return mockAchievements;
}

export async function saveMockAchievements(
  achievementIds: string[],
): Promise<void> {
  await delay();

  const storage = getMockStorage();

  storage.selectedAchievementIds = achievementIds;

  saveMockStorage(storage);
}

export async function getMockPrivileges(): Promise<PrivilegeCategory[]> {
  await delay();

  return mockPrivileges;
}

export async function saveMockPrivileges(
  categoryIds: string[],
): Promise<PrivilegeApplyResult> {
  await delay();

  const storage = getMockStorage();

  storage.selectedPrivilegeIds = categoryIds;

  saveMockStorage(storage);

  const selectedPrivileges = mockPrivileges.filter((privilege) =>
    categoryIds.includes(privilege.id),
  );

  let bestQuotaType: PrivilegeApplyResult["bestQuotaType"] = "none";

  if (selectedPrivileges.some((item) => item.quotaType === "bvi")) {
    bestQuotaType = "bvi";
  } else if (
    selectedPrivileges.some((item) => item.quotaType === "special_quota")
  ) {
    bestQuotaType = "special_quota";
  } else if (
    selectedPrivileges.some((item) => item.quotaType === "separate_quota")
  ) {
    bestQuotaType = "separate_quota";
  } else if (
    selectedPrivileges.some((item) => item.quotaType === "target_quota")
  ) {
    bestQuotaType = "target_quota";
  }

  return {
    selectedCategoryIds: categoryIds,

    bestQuotaType,

    message:
      bestQuotaType === "none"
        ? "Льготы не применяются"
        : "Подходящая льгота определена автоматически",
  };
}

export async function getMockProfile(): Promise<Profile> {
  await delay();

  const storage = getMockStorage();
  const universities = calculateMockUniversities(storage);
  const egeScores = mockSession.egeScores;
  const programs = {
    total: universities.length,
    bvi: 0,
    abovePrevious: universities.filter((item) => item.status === "reserve").length,
    nearPrevious: universities.filter((item) => item.status === "real").length,
    belowPrevious: universities.filter((item) => item.status === "risk").length,
    insufficientData: 0,
  };
  return {
    ...mockProfile,
    egeScores,
    egeTotal: getEgeScoreSummary(egeScores).total,
    interests: mockInterests.filter((item) => storage.selectedInterestIds.includes(item.id)),
    achievements: mockAchievements
      .filter((item) => storage.selectedAchievementIds.includes(item.id))
      .map((item) => item.name),
    programs,
    advice: programs.total
      ? mockProfile.advice
      : "По выбранным направлениям подходящих программ нет. Проверь баллы ЕГЭ или добавь направления.",
  };
}

export async function getMockSubjects(): Promise<Subject[]> {
  await delay();

  return mockSubjects;
}

export async function getMockEgeScores(): Promise<EgeScoresSubmission> {
  await delay();

  return { scores: mockSession.egeScores, allPassed: mockSession.egeScores.every((item) => item.passed) };
}

export async function saveMockEgeScores(scores: EgeScoreInput[]): Promise<EgeScoresSubmission> {
  await delay();

  const result = scores.map((input) => {
    const subject = mockSubjects.find((item) => item.id === input.subjectId);
    const previous = mockSession.egeScores.find((item) => item.subjectId === input.subjectId);
    const minThreshold = subject?.minThreshold ?? previous?.minThreshold ?? 0;
    return {
      subjectId: input.subjectId,
      subjectName: subject?.name ?? previous?.subjectName ?? input.subjectId,
      score: input.score,
      minThreshold,
      passed: input.score >= minThreshold,
    };
  });
  mockSession.egeScores = result;
  return { scores: result, allPassed: result.every((item) => item.passed) };
}

export async function getMockUniversities(): Promise<UniversitySummary[]> {
  await delay();

  const storage = getMockStorage();

  return calculateMockUniversities(storage);
}

export async function searchMockUniversities(
  query: string,
): Promise<UniversityCard[]> {
  await delay();

  const normalizedQuery = query.trim().toLowerCase();

  if (!normalizedQuery) {
    return mockUniversityCards;
  }

  return mockUniversityCards.filter((university) => {
    const name = university.name.toLowerCase();
    const city = university.city.toLowerCase();

    return name.includes(normalizedQuery) || city.includes(normalizedQuery);
  });
}

export async function getMockUniversityDetail(
  universityId: string,
): Promise<UniversityDetail> {
  await delay();

  return calculateMockDetail(universityId, getMockStorage());
}

export async function addMockUniversity(
  universityId: string,
): Promise<UniversitySummary> {
  await delay();

  const storage = getMockStorage();

  const university = mockUniversities.find((item) => item.id === universityId);

  if (!university) {
    throw new Error("University not found");
  }

  if (!storage.universityIds.includes(universityId)) {
    storage.universityIds.push(universityId);

    storage.universityPriorityIds.push(universityId);

    saveMockStorage(storage);
  }

  return calculateMockUniversities(storage).find((item) => item.id === universityId)!;
}

export async function removeMockUniversity(
  universityId: string,
): Promise<void> {
  await delay();

  const storage = getMockStorage();

  storage.universityIds = storage.universityIds.filter(
    (id) => id !== universityId,
  );

  storage.universityPriorityIds = storage.universityPriorityIds.filter(
    (id) => id !== universityId,
  );

  saveMockStorage(storage);
}

export async function getMockPriorities(): Promise<PriorityListItem[]> {
  await delay();

  const storage = getMockStorage();

  return calculateMockUniversities(storage).map((university) => ({
    universityId: university.id,
    universityName: university.name,
    priority: university.priority,
    chancePercent: university.chancePercent,
    myScore: university.myScore,
    passingScorePreviousYear: university.passingScorePreviousYear,
    status: university.status,
  }));
}

export async function saveMockPriorities(
  universityIds: string[],
): Promise<PriorityListItem[]> {
  await delay();

  const storage = getMockStorage();

  if (universityIds.length !== storage.universityIds.length || new Set(universityIds).size !== universityIds.length || universityIds.some((id) => !storage.universityIds.includes(id))) {
    throw new Error("Invalid university priorities");
  }
  storage.universityPriorityIds = universityIds;

  saveMockStorage(storage);

  return getMockPriorities();
}

export async function saveMockDirectionPriorities(
  universityId: string,
  directionIds: string[],
): Promise<UniversityDetail> {
  await delay();

  const university = mockUniversityDetails.find(
    (item) => item.id === universityId,
  );

  if (!university) {
    throw new Error("University not found");
  }

  const newDirections = directionIds
    .map((directionId, index) => {
      const direction = university.directions.find(
        (item) => item.id === directionId,
      );

      if (!direction) {
        return null;
      }

      return {
        ...direction,
        priority: index + 1,
      };
    })
    .filter((item) => item !== null);

  if (newDirections.length !== university.directions.length || new Set(directionIds).size !== university.directions.length) {
    throw new Error("Invalid direction priorities");
  }
  const storage = getMockStorage();
  storage.directionPriorityIds[universityId] = directionIds;
  saveMockStorage(storage);
  return calculateMockDetail(universityId, storage);
}

export { finalizeMockStrategy, getMockStrategyReport };

export async function exchangeMockAuth(): Promise<ExchangeAuthResponse> {
  await delay();

  return {
    accessToken: "mock-access-token",

    expiresIn: 3600,

    session: {
      id: mockSession.id,
      platform: "max",
      countryCode: mockSession.countryCode,
      createdAt: new Date().toISOString(),
    },
  };
}
