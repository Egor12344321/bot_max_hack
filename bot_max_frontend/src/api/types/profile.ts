import type { InterestCategory } from "@/api/types/onboarding";
import type { SavedDiploma, StudyDirection } from "@/api/types/planning";
import type { EgeScoreResult } from "@/api/types/session";

/**
 * Сводка по рекомендациям всех выбранных направлений.
 * Вероятность поступления не считается: по одному прошлогоднему проходному её не оценить.
 */
export interface ProfilePrograms {
  total: number;
  bvi: number;
  abovePrevious: number;
  nearPrevious: number;
  belowPrevious: number;
  insufficientData: number;
}

/** GET /sessions/{id}/profile. Имя пользователя берётся из MAX на клиенте. */
export interface Profile {
  countryCode: string | null;
  egeScores: EgeScoreResult[];
  egeTotal: number;
  interests: InterestCategory[];
  directions: StudyDirection[];
  olympiads: SavedDiploma[];
  achievements: string[];
  privileges: string[];
  programs: ProfilePrograms;
  advice: string;
}
