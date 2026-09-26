import { useEffect, useState } from "react";

import { Typography } from "@maxhub/max-ui";

import { useTranslation } from "react-i18next";

import type { DeadlineEvent } from "@/api/types/deadlines";

import { getDeadlines, saveDeadlineReminders } from "@/api/deadlinesApi";

import { AppLayout } from "@/components/AppLayout/AppLayout";

import { useAppSelector } from "@/store/hooks";

import { formatDate } from "@/utils/formatDate";

import styles from "./CalendarPage.module.css";

export function CalendarPage() {
  const { t, i18n } = useTranslation();

  const sessionId = useAppSelector((state) => state.session.sessionId);

  const [deadlines, setDeadlines] = useState<DeadlineEvent[]>([]);

  const [isLoading, setIsLoading] = useState(true);

  const [error, setError] = useState<string | null>(null);

  const [savingIds, setSavingIds] = useState<string[]>([]);

  useEffect(() => {
    async function loadDeadlines() {
      if (!sessionId) {
        return;
      }

      try {
        setIsLoading(true);
        setError(null);

        const data = await getDeadlines(sessionId);

        setDeadlines(data);
      } catch {
        setError(t("calendar.loadError"));
      } finally {
        setIsLoading(false);
      }
    }

    loadDeadlines();
  }, [sessionId, t]);

  async function handleReminderChange(
    event: DeadlineEvent,
    field: "remind3Days" | "remind24Hours",
  ) {
    if (!sessionId || savingIds.includes(event.id)) {
      return;
    }

    const nextEvent = {
      ...event,
      [field]: !event[field],
    };

    setDeadlines((current) =>
      current.map((item) => (item.id === event.id ? nextEvent : item)),
    );

    try {
      setSavingIds((current) => [...current, event.id]);

      await saveDeadlineReminders(
        sessionId,
        event.id,
        nextEvent.remind3Days,
        nextEvent.remind24Hours,
      );
    } catch {
      setDeadlines((current) =>
        current.map((item) => (item.id === event.id ? event : item)),
      );

      setError(t("calendar.saveError"));
    } finally {
      setSavingIds((current) => current.filter((id) => id !== event.id));
    }
  }

  function getStatusLabel(status: DeadlineEvent["status"]) {
    if (status === "passed") {
      return t("calendar.status.passed");
    }

    if (status === "upcoming") {
      return t("calendar.status.upcoming");
    }

    return t("calendar.status.future");
  }

  if (isLoading) {
    return (
      <AppLayout>
        <div className={styles.state}>
          <Typography.Body>{t("common.loading")}</Typography.Body>
        </div>
      </AppLayout>
    );
  }

  return (
    <AppLayout>
      <div className={styles.page}>
        <header className={styles.header}>
          <Typography.Title>{t("calendar.pageTitle")}</Typography.Title>

          <div className={styles.subtitle}>{t("calendar.description")}</div>
        </header>

        {error && <div className={styles.error}>{error}</div>}

        <div className={styles.timeline}>
          {deadlines.map((event) => {
            const disabled = event.status === "passed";

            const isSaving = savingIds.includes(event.id);

            return (
              <div key={event.id} className={styles.deadline}>
                <div className={`${styles.dot} ${styles[event.status]}`} />

                <div className={`${styles.card} ${styles[event.status]}`}>
                  <div className={styles.top}>
                    <div>
                      <div className={`${styles.date} ${styles[event.status]}`}>
                        {formatDate(event.date, i18n.language)}
                      </div>

                      <div className={styles.title}>{event.title}</div>
                    </div>

                    <span className={styles.badge}>
                      {getStatusLabel(event.status)}
                    </span>
                  </div>

                  {!disabled && (
                    <div className={styles.reminders}>
                      <label className={styles.reminderRow}>
                        <span className={styles.reminderLabel}>
                          {t("calendar.remind3Days")}
                        </span>

                        <input
                          type="checkbox"
                          className={styles.switch}
                          checked={event.remind3Days}
                          disabled={isSaving}
                          onChange={() =>
                            handleReminderChange(event, "remind3Days")
                          }
                        />
                      </label>

                      <label className={styles.reminderRow}>
                        <span className={styles.reminderLabel}>
                          {t("calendar.remind24Hours")}
                        </span>

                        <input
                          type="checkbox"
                          className={styles.switch}
                          checked={event.remind24Hours}
                          disabled={isSaving}
                          onChange={() =>
                            handleReminderChange(event, "remind24Hours")
                          }
                        />
                      </label>
                    </div>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </AppLayout>
  );
}
