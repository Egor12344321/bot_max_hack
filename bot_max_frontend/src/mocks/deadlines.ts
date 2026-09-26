import type { DeadlineEvent } from "@/api/types/deadlines";

export const mockDeadlines: DeadlineEvent[] = [
  {
    id: "documents-start",
    date: "2027-06-20",
    title: "Начало приёма документов",

    status: "upcoming",

    remind3Days: true,
    remind24Hours: true,
  },
  {
    id: "documents-budget-end",
    date: "2027-07-25",
    title: "Окончание приёма документов на бюджет",

    status: "future",

    remind3Days: true,
    remind24Hours: false,
  },
  {
    id: "priority-deadline",
    date: "2027-08-01",
    title: "Последний день подачи согласия на зачисление",

    status: "future",

    remind3Days: true,
    remind24Hours: true,
  },
  {
    id: "enrollment",
    date: "2027-08-05",
    title: "Публикация приказов о зачислении",

    status: "future",

    remind3Days: false,
    remind24Hours: true,
  },
];
