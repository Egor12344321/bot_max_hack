import { useEffect, useState } from "react";

import { Button, Typography } from "@maxhub/max-ui";

import { useNavigate } from "react-router-dom";

import { useTranslation } from "react-i18next";

import type { UniversitySummary } from "@/api/types/universities";

import {
  getUniversities,
  getUniversityDetail,
  removeUniversity,
} from "@/api/universitiesApi";

import { AppLayout } from "@/components/AppLayout/AppLayout";

import { UniversityCard } from "@/components/UniversityCard/UniversityCard";

import { useAppSelector } from "@/store/hooks";

import styles from "./UniversitiesPage.module.css";

interface TopDirection {
  name: string;
  passingScore: number;
}

export function UniversitiesPage() {
  const { t } = useTranslation();

  const navigate = useNavigate();

  const sessionId = useAppSelector((state) => state.session.sessionId);

  const [universities, setUniversities] = useState<UniversitySummary[]>([]);

  const [topDirections, setTopDirections] = useState<
    Record<string, TopDirection>
  >({});

  const [isLoading, setIsLoading] = useState(true);

  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    async function loadUniversities() {
      if (!sessionId) {
        return;
      }

      try {
        setIsLoading(true);
        setError(null);

        const data = await getUniversities(sessionId);

        setUniversities([...data].sort((a, b) => a.priority - b.priority));

        const details = await Promise.all(
          data.map((university) =>
            getUniversityDetail(sessionId, university.id),
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
        setError(t("universities.loadError"));
      } finally {
        setIsLoading(false);
      }
    }

    loadUniversities();
  }, [sessionId, t]);

  function handleOpen(universityId: string) {
    navigate(`/universities/${universityId}`);
  }

  async function handleRemove(universityId: string) {
    if (!sessionId) {
      return;
    }

    try {
      await removeUniversity(sessionId, universityId);

      setUniversities((current) =>
        current
          .filter((university) => university.id !== universityId)
          .map((university, index) => ({ ...university, priority: index + 1 })),
      );

      setTopDirections((current) => {
        const next = {
          ...current,
        };

        delete next[universityId];

        return next;
      });
    } catch {
      setError(t("universities.removeError"));
    }
  }

  return (
    <AppLayout>
      <div className={styles.page}>
        <header className={styles.header}>
          <div>
            <Typography.Title>
              {t("universities.myUniversities")}
            </Typography.Title>

            <div className={styles.count}>
              {t("universities.selectedCount", {
                count: universities.length,
              })}
            </div>
          </div>

          <Button variant="secondary" onClick={() => navigate("/universities/search")}>
            + {t("universities.add")}
          </Button>
        </header>

        {isLoading && (
          <div className={styles.state}>
            <Typography.Body>{t("common.loading")}</Typography.Body>
          </div>
        )}

        {error && <div className={styles.error}>{error}</div>}

        {!isLoading && universities.length === 0 && (
          <div className={styles.empty}>
            <div className={styles.emptyIcon}>🏛️</div>

            <Typography.Title>{t("universities.emptyTitle")}</Typography.Title>

            <Typography.Body>
              {t("universities.emptyDescription")}
            </Typography.Body>
          </div>
        )}

        {!isLoading && universities.length > 0 && (
          <div className={styles.list}>
            {universities.map((university) => (
              <UniversityCard
                key={university.id}
                university={university}
                topDirection={topDirections[university.id]}
                onOpen={() => handleOpen(university.id)}
                onRemove={() => handleRemove(university.id)}
              />
            ))}
          </div>
        )}
      </div>
    </AppLayout>
  );
}
