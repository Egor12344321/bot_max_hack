const STORAGE_KEY = "max-abiturient-mock";

import type { StrategyFinalizeResponse } from "@/api/types/strategy";

export interface MockStorageData {
  selectedInterestIds: string[];
  selectedAchievementIds: string[];
  selectedPrivilegeIds: string[];

  universityIds: string[];

  universityPriorityIds: string[];
  directionPriorityIds: Record<string, string[]>;
  strategyReport: StrategyFinalizeResponse | null;

  deadlineReminders: Record<
    string,
    {
      remind3Days: boolean;
      remind24Hours: boolean;
    }
  >;
}

const defaultData: MockStorageData = {
  selectedInterestIds: [],
  selectedAchievementIds: [],
  selectedPrivilegeIds: [],

  universityIds: ["itmo", "hse", "msu", "mipt"],

  universityPriorityIds: ["itmo", "hse", "msu", "mipt"],
  directionPriorityIds: {},
  strategyReport: null,

  deadlineReminders: {},
};

export function getMockStorage(): MockStorageData {
  const savedData = localStorage.getItem(STORAGE_KEY);

  if (!savedData) {
    return structuredClone(defaultData);
  }

  try {
    const parsedData = JSON.parse(savedData) as MockStorageData;

    return {
      ...structuredClone(defaultData),
      ...parsedData,
    };
  } catch {
    return structuredClone(defaultData);
  }
}

export function saveMockStorage(data: MockStorageData) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(data));
}

export function resetMockStorage() {
  localStorage.removeItem(STORAGE_KEY);
}
