import type { Profile } from "@/api/types/profile";
import type { Subject } from "@/api/types/session";

export const mockProfile: Profile = {
  language: "ru",
  countryCode: "RU",

  egeScores: [],
  egeTotal: 0,

  interests: [],
  directions: [
    { id: "09.03.04", code: "09.03.04", name: "Программная инженерия" },
  ],
  olympiads: [],
  achievements: [],
  privileges: [],

  programs: {
    total: 0,
    bvi: 0,
    abovePrevious: 0,
    nearPrevious: 0,
    belowPrevious: 0,
    insufficientData: 0,
  },

  advice: "Подборка готова. Открой её, чтобы собрать план поступления.",
};

export const mockSubjects: Subject[] = [
  { id: "russian", name: "Русский язык", minThreshold: 36 },
  { id: "math-profile", name: "Математика (профиль)", minThreshold: 27 },
  { id: "informatics", name: "Информатика", minThreshold: 40 },
  { id: "physics", name: "Физика", minThreshold: 36 },
  { id: "chemistry", name: "Химия", minThreshold: 36 },
  { id: "biology", name: "Биология", minThreshold: 36 },
  { id: "social-studies", name: "Обществознание", minThreshold: 42 },
];
