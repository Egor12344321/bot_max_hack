export interface InterestCategory {
  id: string;
  name: string;
  icon: string;
}

export interface SetInterestsRequest {
  categoryIds: string[];
}

export interface Achievement {
  id: string;
  name: string;
  description: string | null;
}

export interface SetAchievementsRequest {
  achievementIds: string[];
}

export type QuotaType =
  | "bvi"
  | "special_quota"
  | "separate_quota"
  | "target_quota"
  | "none";

export interface PrivilegeCategory {
  id: string;
  name: string;
  quotaType: QuotaType;
  maxQuotaPercent: number | null;
  requiredDocuments: string[];
}

export interface SetPrivilegesRequest {
  categoryIds: string[];
}

export interface PrivilegeApplyResult {
  selectedCategoryIds: string[];
  bestQuotaType: QuotaType;
  message: string;
}
