import { useEffect, useState } from "react";

import { Button, Typography } from "@maxhub/max-ui";

import { useNavigate, useParams } from "react-router-dom";

import { useTranslation } from "react-i18next";

import type { Direction, UniversityDetail } from "@/api/types/universities";

import {
  getUniversities,
  getUniversityDetail,
  saveDirectionPriorities,
} from "@/api/universitiesApi";

import { useAppSelector } from "@/store/hooks";

import styles from "./UniversityDetailPage.module.css";

export function UniversityDetailPage() {
  const navigate = useNavigate();

  const { universityId } = useParams();

  const { t } = useTranslation();

  const sessionId = useAppSelector((state) => state.session.sessionId);

  const [university, setUniversity] = useState<UniversityDetail | null>(null);
  const [myScore, setMyScore] = useState<number | null>(null);

  const [directions, setDirections] = useState<Direction[]>([]);

  const [isLoading, setIsLoading] = useState(true);

  const [isSaving, setIsSaving] = useState(false);

  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    async function loadUniversity() {
      if (!sessionId || !universityId) {
        return;
      }

      try {
        setIsLoading(true);
        setError(null);

        const [data, summaries] = await Promise.all([
          getUniversityDetail(sessionId, universityId),
          getUniversities(sessionId),
        ]);

        setUniversity(data);
        setMyScore(summaries.find((item) => item.id === universityId)?.myScore ?? null);

        const sortedDirections = [...data.directions].sort(
          (a, b) => a.priority - b.priority,
        );

        setDirections(sortedDirections);
      } catch {
        setError(t("universityDetail.loadError"));
      } finally {
        setIsLoading(false);
      }
    }

    loadUniversity();
  }, [sessionId, universityId, t]);

  function moveDirectionUp(index: number) {
    if (index === 0) {
      return;
    }

    const newDirections = [...directions];

    const current = newDirections[index];

    const previous = newDirections[index - 1];

    newDirections[index - 1] = current;

    newDirections[index] = previous;

    setDirections(newDirections);
  }

  function moveDirectionDown(index: number) {
    if (index === directions.length - 1) {
      return;
    }

    const newDirections = [...directions];

    const current = newDirections[index];

    const next = newDirections[index + 1];

    newDirections[index + 1] = current;

    newDirections[index] = next;

    setDirections(newDirections);
  }

  async function handleSave() {
    if (!sessionId || !universityId) {
      return;
    }

    try {
      setIsSaving(true);
      setError(null);

      const directionIds = directions.map((direction) => direction.id);

      const updatedDirections = await saveDirectionPriorities(
        sessionId,
        universityId,
        directionIds,
      );

      setDirections(updatedDirections);
    } catch {
      setError(t("universityDetail.saveError"));
    } finally {
      setIsSaving(false);
    }
  }

  if (isLoading) {
    return (
      <div className={styles.page}>
        <div className={styles.state}>
          <Typography.Body>{t("common.loading")}</Typography.Body>
        </div>
      </div>
    );
  }

  if (error && !university) {
    return (
      <div className={styles.page}>
        <div className={styles.state}>
          <Typography.Body>{error}</Typography.Body>
        </div>
      </div>
    );
  }

  if (!university) {
    return null;
  }

  return (
    <div className={styles.page}>
      <header className={styles.header}>
        <button
          type="button"
          className={styles.backButton}
          onClick={() => navigate("/universities")}
        >
          ←
        </button>

        <div>
          <div className={styles.title}>{university.name}</div>

          <div className={styles.subtitle}>
            {university.chancePercent}% {t("universityDetail.chance")}
          </div>
        </div>
      </header>

      <main className={styles.content}>
        <section className={styles.achievementCard}>
          <div className={styles.achievementHeader}>
            <div className={styles.achievementTitle}>
              {t("universityDetail.achievementTitle")}
            </div>

            <div className={styles.achievementTotal}>
              +{university.idScoreTotal} {t("universityDetail.pointsShort")}
            </div>
          </div>

          <div className={styles.breakdown}>
            {university.breakdown.map((item) => (
              <div key={item.achievementId} className={styles.breakdownRow}>
                <div>
                  <div className={styles.breakdownName}>
                    {item.achievementName}
                  </div>

                  {item.note && (
                    <div className={styles.breakdownNote}>{item.note}</div>
                  )}
                </div>

                <div
                  className={item.counted ? styles.points : styles.pointsMuted}
                >
                  {item.counted ? `+${item.points}` : "0"}
                </div>
              </div>
            ))}
          </div>

          <div className={styles.achievementNote}>
            {t("universityDetail.maxAchievement", {
              max: university.idScoreMax,
            })}
          </div>
        </section>

        <section>
          <div className={styles.directionsHeader}>
            <div className={styles.directionsTitle}>
              {t("universityDetail.directionsTitle")}
            </div>

            <div className={styles.directionsDescription}>
              {t("universityDetail.directionsDescription")}
            </div>
          </div>

          <div className={styles.directionsList}>
            {directions.map((direction, index) => (
              <div key={direction.id} className={styles.directionCard}>
                <div className={styles.directionPriority}>{index + 1}</div>

                <div className={styles.directionInfo}>
                  <div className={styles.directionName}>{direction.name}</div>

                  <div className={styles.directionMeta}>
                    <span>
                      {t("universities.myScore")}: {myScore ?? "—"}
                    </span>

                    <span>·</span>

                    <span>
                      {direction.chancePercent}% {t("universityDetail.chance")}
                    </span>

                    <span>·</span>

                    <span>
                      {t("universityDetail.passingScore")}:{" "}
                      {direction.passingScorePreviousYear}
                    </span>
                  </div>
                </div>

                <div className={styles.arrows}>
                  <button
                    type="button"
                    className={styles.arrowButton}
                    disabled={isSaving || index === 0}
                    onClick={() => moveDirectionUp(index)}
                  >
                    ↑
                  </button>

                  <button
                    type="button"
                    className={styles.arrowButton}
                    disabled={isSaving || index === directions.length - 1}
                    onClick={() => moveDirectionDown(index)}
                  >
                    ↓
                  </button>
                </div>
              </div>
            ))}
          </div>
        </section>

        {error && <div className={styles.error}>{error}</div>}

        <Button stretched disabled={isSaving} onClick={handleSave}>
          {isSaving ? t("common.saving") : t("universityDetail.save")}
        </Button>
      </main>
    </div>
  );
}
