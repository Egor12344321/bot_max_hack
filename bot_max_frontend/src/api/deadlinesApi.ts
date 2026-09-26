import type {
  DeadlineEvent,
  UpdateDeadlineRemindersRequest,
} from "@/api/types/deadlines";

import { request } from "@/api/client";

import { getMockDeadlines, saveMockDeadlineReminders } from "@/mocks/mockApi";

const isMock = import.meta.env.VITE_API_MODE === "mock";

export async function getDeadlines(
  sessionId: string,
): Promise<DeadlineEvent[]> {
  if (isMock) {
    return getMockDeadlines();
  }

  return request<DeadlineEvent[]>(`/sessions/${sessionId}/deadlines`);
}

export async function saveDeadlineReminders(
  sessionId: string,
  eventId: string,
  remind3Days: boolean,
  remind24Hours: boolean,
): Promise<DeadlineEvent> {
  if (isMock) {
    return saveMockDeadlineReminders(eventId, remind3Days, remind24Hours);
  }

  const body: UpdateDeadlineRemindersRequest = {
    remind3Days,
    remind24Hours,
  };

  return request<DeadlineEvent>(
    `/sessions/${sessionId}/deadlines/${eventId}/reminders`,
    {
      method: "PATCH",
      body: JSON.stringify(body),
    },
  );
}
