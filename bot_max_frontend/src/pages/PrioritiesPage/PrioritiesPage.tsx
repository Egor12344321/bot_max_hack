import { useEffect, useState } from "react";

import { Button, Typography } from "@maxhub/max-ui";

import { useNavigate } from "react-router-dom";

import { useTranslation } from "react-i18next";

import type { PriorityListItem } from "@/api/types/priorities";

import { finalizeStrategy } from "@/api/strategyApi";

import { getPriorities, savePriorities } from "@/api/prioritiesApi";

import { getUniversityDetail } from "@/api/universitiesApi";

import { AppLayout } from "@/components/AppLayout/AppLayout";

import { useAppSelector } from "@/store/hooks";

import styles from "./PrioritiesPage.module.css";

interface TopDirection {
  name: string;
  passingScore: number;
}

export function PrioritiesPage() {
  const { t } = useTranslation();

  const navigate = useNavigate();

  const sessionId = useAppSelector((state) => state.session.sessionId);

  const [priorities, setPriorities] = useState<PriorityListItem[]>([]);

  const [topDirections, setTopDirections] = useState<
    Record<string, TopDirection>
  >({});

  const [isLoading, setIsLoading] = useState(true);

  const [isSaving, setIsSaving] = useState(false);

  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    async function loadPriorities() {
      if (!sessionId) {
        return;
      }

      try {
        setIsLoading(true);
        setError(null);

        const data = await getPriorities(sessionId);

        const sorted = [...data].sort((a, b) => a.priority - b.priority);

        setPriorities(sorted);

        const details = await Promise.all(
          sorted.map((item) =>
            getUniversityDetail(sessionId, item.universityId),
          ),
        );

        const directionsMap: Record<string, TopDirection> = {};

        details.forEach((detail) => {
          const sortedDirections = [...detail.directions].sort(
            (a, b) => a.priority - b.priority,
          );

          const firstDirection = sortedDirections[0];

          if (firstDirection) {
            directionsMap[detail.id] = {
              name: firstDirection.name,

              passingScore: firstDirection.passingScorePreviousYear,
            };
          }
        });

        setTopDirections(directionsMap);
      } catch {
        setError(t("priorities.loadError"));
      } finally {
        setIsLoading(false);
      }
    }

    loadPriorities();
  }, [sessionId, t]);

  function moveUp(index: number) {
    if (index === 0) {
      return;
    }

    const newPriorities = [...priorities];

    const current = newPriorities[index];

    const previous = newPriorities[index - 1];

    newPriorities[index - 1] = current;

    newPriorities[index] = previous;

    setPriorities(newPriorities);
  }

  function moveDown(index: number) {
    if (index === priorities.length - 1) {
      return;
    }

    const newPriorities = [...priorities];

    const current = newPriorities[index];

    const next = newPriorities[index + 1];

    newPriorities[index + 1] = current;

    newPriorities[index] = next;

    setPriorities(newPriorities);
  }

  async function handleSave() {
    if (!sessionId) {
      return;
    }

    try {
      setIsSaving(true);
      setError(null);

      const universityIds = priorities.map((item) => item.universityId);

      const updated = await savePriorities(sessionId, universityIds);

      setPriorities([...updated].sort((a, b) => a.priority - b.priority));

      await finalizeStrategy(sessionId);

      navigate("/strategy/report");
    } catch {
      setError(t("priorities.saveError"));
    } finally {
      setIsSaving(false);
    }
  }

  function getStatusText(status: PriorityListItem["status"]) {
    if (status === "reserve") {
      return t("priorities.status.reserve");
    }

    if (status === "real") {
      return t("priorities.status.real");
    }

    return t("priorities.status.risk");
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
          <Typography.Title>{t("priorities.pageTitle")}</Typography.Title>

          <div className={styles.subtitle}>{t("priorities.description")}</div>
        </header>

        <div className={styles.info}>


          <span>{t("priorities.tip")}</span>
        </div>

        {error && <div className={styles.error}>{error}</div>}

        {priorities.length === 0 && (
          <div className={styles.empty}>


            <Typography.Title>{t("priorities.emptyTitle")}</Typography.Title>

            <Typography.Body>
              {t("priorities.emptyDescription")}
            </Typography.Body>

            <Button stretched onClick={() => navigate("/universities")}>
              {t("priorities.openUniversities")}
            </Button>
          </div>
        )}

        {priorities.length > 0 && (
          <>
            <div className={styles.list}>
              {priorities.map((item, index) => {
                const topDirection = topDirections[item.universityId];

                const passingScore =
                  topDirection?.passingScore ?? item.passingScorePreviousYear;

                return (
                  <div key={item.universityId} className={styles.card}>
                    <button
                      type="button"
                      className={styles.cardContent}
                      onClick={() =>
                        navigate(`/universities/${item.universityId}`)
                      }
                    >
                      <div className={styles.priorityNumber}>{index + 1}</div>

                      <div className={styles.infoBlock}>
                        <div className={styles.universityName}>
                          {item.universityName}
                        </div>

                        {topDirection && (
                          <div className={styles.topDirection}>
                            {topDirection.name}
                          </div>
                        )}

                        <div className={styles.meta}>
                          <span className={`${styles.status} ${styles[item.status]}`}>
                            {getStatusText(item.status)}
                          </span>

                          <span>{item.chancePercent}%</span>
                        </div>

                        <div className={styles.scores}>
                          <span>
                            {t("priorities.myScore")}:{" "}
                            <strong>{item.myScore}</strong>
                          </span>

                          <span>·</span>

                          <span>
                            {t("priorities.passingScore")}:{" "}
                            <strong>{passingScore}</strong>
                          </span>
                        </div>
                      </div>
                    </button>

                    <div className={styles.arrows}>
                      <button
                        type="button"
                        className={styles.arrowButton}
                        disabled={isSaving || index === 0}
                        onClick={() => moveUp(index)}
                      >
                        ↑
                      </button>

                      <button
                        type="button"
                        className={styles.arrowButton}
                        disabled={isSaving || index === priorities.length - 1}
                        onClick={() => moveDown(index)}
                      >
                        ↓
                      </button>
                    </div>
                  </div>
                );
              })}
            </div>

            <Button stretched disabled={isSaving} onClick={handleSave}>
              {isSaving ? t("common.saving") : t("priorities.save")}
            </Button>
          </>
        )}
      </div>
    </AppLayout>
  );
}
