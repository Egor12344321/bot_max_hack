import type {
  UniversityCard,
  UniversityDetail,
  UniversitySummary,
} from "@/api/types/universities";
import { mockSession } from "@/mocks/session";
import { getEgeScoreSummary } from "@/utils/egeScore";

export const mockUniversityCards: UniversityCard[] = [
  {
    id: "msu",
    name: "МГУ",
    city: "Москва",
    hasDormitory: true,
    hasMilitaryDept: true,
    directionsCount: 3,
  },
  {
    id: "hse",
    name: "НИУ ВШЭ",
    city: "Москва",
    hasDormitory: true,
    hasMilitaryDept: false,
    directionsCount: 4,
  },
  {
    id: "itmo",
    name: "ИТМО",
    city: "Санкт-Петербург",
    hasDormitory: true,
    hasMilitaryDept: true,
    directionsCount: 3,
  },
  {
    id: "mipt",
    name: "МФТИ",
    city: "Долгопрудный",
    hasDormitory: true,
    hasMilitaryDept: true,
    directionsCount: 2,
  },
];

const universitySummaries: Omit<UniversitySummary, "myScore">[] = [
  {
    id: "itmo",
    name: "ИТМО",
    priority: 1,
    directionsCount: 3,

    passingScorePreviousYear: 276,

    chancePercent: 82,
    status: "reserve",

    seatsTotal: 120,
    seatsFilled: 84,

    tags: ["IT", "Информатика"],
  },
  {
    id: "hse",
    name: "НИУ ВШЭ",
    priority: 2,
    directionsCount: 4,

    passingScorePreviousYear: 281,

    chancePercent: 71,
    status: "real",

    seatsTotal: 100,
    seatsFilled: 81,

    tags: ["IT", "Экономика"],
  },
  {
    id: "msu",
    name: "МГУ",
    priority: 3,
    directionsCount: 3,

    passingScorePreviousYear: 289,

    chancePercent: 54,
    status: "real",

    seatsTotal: 90,
    seatsFilled: 78,

    tags: ["Математика", "IT"],
  },
  {
    id: "mipt",
    name: "МФТИ",
    priority: 4,
    directionsCount: 2,

    passingScorePreviousYear: 296,

    chancePercent: 34,
    status: "risk",

    seatsTotal: 80,
    seatsFilled: 73,

    tags: ["Физика", "IT"],
  },
];

export const mockUniversityDetails: UniversityDetail[] = [
  {
    id: "itmo",
    name: "ИТМО",

    chancePercent: 82,

    idScoreTotal: 10,
    idScoreMax: 10,

    breakdown: [
      {
        achievementId: "gto_gold",
        achievementName: "Золотой знак ГТО",
        points: 3,
        counted: true,
        note: null,
      },
      {
        achievementId: "school_medal",
        achievementName: "Медаль за успехи в учёбе",
        points: 7,
        counted: true,
        note: null,
      },
      {
        achievementId: "volunteering",
        achievementName: "Волонтёрская деятельность",
        points: 0,
        counted: false,
        note: "Максимум за индивидуальные достижения уже достигнут",
      },
    ],

    directions: [
      {
        id: "itmo-software",
        universityId: "itmo",
        name: "Программная инженерия",

        priority: 1,

        passingScorePreviousYear: 278,
        chancePercent: 80,

        riskFactors: {
          passingScorePreviousYear: 278,
          budgetPlacesPreviousYear: 110,
          budgetPlacesCurrentYear: 120,
          competitionIndex: 8.4,
        },
      },
      {
        id: "itmo-ai",
        universityId: "itmo",
        name: "Искусственный интеллект",

        priority: 2,

        passingScorePreviousYear: 284,
        chancePercent: 69,

        riskFactors: {
          passingScorePreviousYear: 284,
          budgetPlacesPreviousYear: 70,
          budgetPlacesCurrentYear: 80,
          competitionIndex: 9.1,
        },
      },
      {
        id: "itmo-infosec",
        universityId: "itmo",
        name: "Информационная безопасность",

        priority: 3,

        passingScorePreviousYear: 270,
        chancePercent: 86,

        riskFactors: {
          passingScorePreviousYear: 270,
          budgetPlacesPreviousYear: 60,
          budgetPlacesCurrentYear: 70,
          competitionIndex: 7.5,
        },
      },
    ],
  },

  {
    id: "hse",
    name: "НИУ ВШЭ",

    chancePercent: 71,

    idScoreTotal: 8,
    idScoreMax: 10,

    breakdown: [
      {
        achievementId: "school_medal",
        achievementName: "Медаль за успехи в учёбе",
        points: 5,
        counted: true,
        note: null,
      },
      {
        achievementId: "gto_gold",
        achievementName: "Золотой знак ГТО",
        points: 3,
        counted: true,
        note: null,
      },
    ],

    directions: [
      {
        id: "hse-software",
        universityId: "hse",
        name: "Программная инженерия",

        priority: 1,

        passingScorePreviousYear: 281,
        chancePercent: 71,

        riskFactors: {
          passingScorePreviousYear: 281,
          budgetPlacesPreviousYear: 80,
          budgetPlacesCurrentYear: 90,
          competitionIndex: 8.8,
        },
      },
      {
        id: "hse-data",
        universityId: "hse",
        name: "Анализ данных",

        priority: 2,

        passingScorePreviousYear: 287,
        chancePercent: 57,

        riskFactors: {
          passingScorePreviousYear: 287,
          budgetPlacesPreviousYear: 60,
          budgetPlacesCurrentYear: 65,
          competitionIndex: 9.4,
        },
      },
    ],
  },

  {
    id: "msu",
    name: "МГУ",

    chancePercent: 54,

    idScoreTotal: 6,
    idScoreMax: 10,

    breakdown: [
      {
        achievementId: "school_medal",
        achievementName: "Медаль за успехи в учёбе",
        points: 6,
        counted: true,
        note: null,
      },
      {
        achievementId: "gto_gold",
        achievementName: "Золотой знак ГТО",
        points: 0,
        counted: false,
        note: "Это достижение не учитывается на выбранном направлении",
      },
    ],

    directions: [
      {
        id: "msu-math",
        universityId: "msu",
        name: "Прикладная математика",

        priority: 1,

        passingScorePreviousYear: 289,
        chancePercent: 54,

        riskFactors: {
          passingScorePreviousYear: 289,
          budgetPlacesPreviousYear: 70,
          budgetPlacesCurrentYear: 70,
          competitionIndex: 9.6,
        },
      },
      {
        id: "msu-informatics",
        universityId: "msu",
        name: "Фундаментальная информатика",

        priority: 2,

        passingScorePreviousYear: 292,
        chancePercent: 48,

        riskFactors: {
          passingScorePreviousYear: 292,
          budgetPlacesPreviousYear: 50,
          budgetPlacesCurrentYear: 55,
          competitionIndex: 9.8,
        },
      },
    ],
  },

  {
    id: "mipt",
    name: "МФТИ",

    chancePercent: 34,

    idScoreTotal: 7,
    idScoreMax: 10,

    breakdown: [
      {
        achievementId: "school_medal",
        achievementName: "Медаль за успехи в учёбе",
        points: 5,
        counted: true,
        note: null,
      },
      {
        achievementId: "gto_gold",
        achievementName: "Золотой знак ГТО",
        points: 2,
        counted: true,
        note: null,
      },
    ],

    directions: [
      {
        id: "mipt-applied-math",
        universityId: "mipt",
        name: "Прикладная математика и информатика",

        priority: 1,

        passingScorePreviousYear: 296,
        chancePercent: 34,

        riskFactors: {
          passingScorePreviousYear: 296,
          budgetPlacesPreviousYear: 90,
          budgetPlacesCurrentYear: 95,
          competitionIndex: 10.2,
        },
      },
    ],
  },
];

// Demo totals use each university's own ID bonus, not a global profile bonus.
export const mockUniversities: UniversitySummary[] = universitySummaries.map(
  (university) => {
    const detail = mockUniversityDetails.find((item) => item.id === university.id);
    if (!detail) {
      throw new Error(`Missing mock university detail: ${university.id}`);
    }

    return {
      ...university,
      myScore: getEgeScoreSummary(mockSession.egeScores).total + detail.idScoreTotal,
    };
  },
);
