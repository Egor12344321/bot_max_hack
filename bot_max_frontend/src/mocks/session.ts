import type { SessionDraft } from "@/api/types/session";

export const mockSession: SessionDraft = {
  id: "mock-session-1",
  countryCode: "RU",

  egeScores: [
    {
      subjectId: "russian",
      subjectName: "Русский язык",
      score: 92,
      minThreshold: 40,
      passed: true,
    },
    {
      subjectId: "math",
      subjectName: "Математика",
      score: 88,
      minThreshold: 40,
      passed: true,
    },
    {
      subjectId: "informatics",
      subjectName: "Информатика",
      score: 95,
      minThreshold: 44,
      passed: true,
    },
  ],

  isCompleteFromBot: true,
};
