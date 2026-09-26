export type DeadlineStatus = "passed" | "upcoming" | "future";

export interface DeadlineEvent {
  id: string;

  date: string;
  title: string;

  status: DeadlineStatus;

  remind3Days: boolean;
  remind24Hours: boolean;
}

export interface UpdateDeadlineRemindersRequest {
  remind3Days: boolean;
  remind24Hours: boolean;
}
