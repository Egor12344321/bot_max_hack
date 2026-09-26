import { useTranslation } from "react-i18next";

import type { UniversitySummary } from "@/api/types/universities";

import styles from "./UniversityCard.module.css";

interface TopDirection {
  name: string;
  passingScore: number;
}

interface UniversityCardProps {
  university: UniversitySummary;

  topDirection?: TopDirection;

  onOpen: () => void;
  onRemove: () => void;
}

export function UniversityCard({
  university,
  topDirection,
  onOpen,
  onRemove,
}: UniversityCardProps) {
  const { t } = useTranslation();

  let statusText = "";

  if (university.status === "reserve") {
    statusText = t("universities.status.reserve");
  }

  if (university.status === "real") {
    statusText = t("universities.status.real");
  }

  if (university.status === "risk") {
    statusText = t("universities.status.risk");
  }

  const passingScore =
    topDirection?.passingScore ?? university.passingScorePreviousYear;

  return (
    <article className={styles.card}>
      <button type="button" className={styles.body} onClick={onOpen}>
        <div className={styles.top}>
          <div>
            <div className={styles.nameRow}>
              <span className={styles.priority}>{university.priority}</span>

              <span className={styles.name}>{university.name}</span>
            </div>

            <div className={styles.directions}>
              {t("universities.directionsCount", {
                count: university.directionsCount,
              })}
            </div>
          </div>

          <div className={styles.badges}>
            <span className={`${styles.status} ${styles[university.status]}`}>
              {statusText}
            </span>

            <span className={`${styles.percent} ${styles[university.status]}`}>
              {university.chancePercent}%
            </span>
          </div>
        </div>

        {topDirection && (
          <div className={styles.topDirection}>
            <span className={styles.topDirectionLabel}>
              {t("universities.firstPriority")}
            </span>

            <span className={styles.topDirectionName}>{topDirection.name}</span>
          </div>
        )}

        <div className={styles.scores}>
          <div className={styles.scoreItem}>
            <div className={styles.myScore}>{university.myScore}</div>

            <div className={styles.scoreLabel}>{t("universities.myScore")}</div>
          </div>

          <div className={styles.vs}>
            <div className={styles.line} />

            <span>vs</span>

            <div className={styles.line} />
          </div>

          <div className={styles.scoreItem}>
            <div className={styles.score}>{passingScore}</div>

            <div className={styles.scoreLabel}>
              {t("universities.passingScore")}
            </div>
          </div>
        </div>

        {university.tags.length > 0 && (
          <div className={styles.tags}>
            {university.tags.map((tag) => (
              <span key={tag} className={styles.tag}>
                {tag}
              </span>
            ))}
          </div>
        )}
      </button>

      <div className={styles.actions}>
        <button
          type="button"
          className={styles.removeButton}
          onClick={onRemove}
        >
          {t("universities.remove")}
        </button>

        <button type="button" className={styles.openButton} onClick={onOpen}>
          {t("universities.details")}
        </button>
      </div>
    </article>
  );
}
