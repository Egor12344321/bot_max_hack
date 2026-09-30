import { useEffect, useState } from "react";

import { Typography } from "@maxhub/max-ui";

import { useNavigate } from "react-router-dom";

import type { Achievement } from "@/api/types/onboarding";

import { getAchievements, saveAchievements } from "@/api/onboardingApi";

import { OnboardingLayout } from "@/components/OnboardingLayout/OnboardingLayout";

import { useAppSelector } from "@/store/hooks";
import { useOnboardingSelection } from "@/hooks/useOnboardingSelection";

import styles from "./AchievementsPage.module.css";

export function AchievementsPage() {
  const navigate = useNavigate();

const sessionId = useAppSelector((state) => state.session.sessionId);

  const [achievements, setAchievements] = useState<Achievement[]>([]);

  const [selectedIds, setSelectedIds] = useOnboardingSelection(
    "selectedAchievementIds",
  );

  const [isLoading, setIsLoading] = useState(true);
  const [loadFailed, setLoadFailed] = useState(false);

  const [isSaving, setIsSaving] = useState(false);

  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    async function loadAchievements() {
      try {
        setIsLoading(true);
        setLoadFailed(false);
        setError(null);

        const data = await getAchievements();

        setAchievements(data);
      } catch {
        setLoadFailed(true);
        setError("Не удалось загрузить достижения");
      } finally {
        setIsLoading(false);
      }
    }

    loadAchievements();
  }, []);

  function handleSelect(achievementId: string) {
    if (selectedIds.includes(achievementId)) {
      setSelectedIds(selectedIds.filter((id) => id !== achievementId));

      return;
    }

    setSelectedIds([...selectedIds, achievementId]);
  }

  async function handleContinue() {
    if (!sessionId) {
      return;
    }

    try {
      setIsSaving(true);
      setError(null);

      await saveAchievements(sessionId, selectedIds);

      navigate("/onboarding/privileges");
    } catch {
      setError("Не удалось сохранить достижения");
    } finally {
      setIsSaving(false);
    }
  }

  return (
    <OnboardingLayout
      step={3}
      totalSteps={4}
      title={"Твои достижения"}
      description={"Отметь, что у тебя есть. Баллы за достижения отличаются в разных вузах."}
      buttonText={"Далее — льготы"}
      buttonLoading={isSaving}
      buttonDisabled={isLoading || loadFailed}
      onButtonClick={handleContinue}
      onBack={() => {
        if (!isSaving) navigate("/onboarding/olympiads");
      }}
    >
      {isLoading && (
        <div className={styles.state}>
          <Typography.Body>{"Загрузка..."}</Typography.Body>
        </div>
      )}

      {error && <div className={styles.error}>{error}</div>}

      {!isLoading && (
        <>
          <div className={styles.card}>
            <div className={styles.cardTitle}>
              {"Индивидуальные достижения"}
            </div>

            {achievements.map((achievement) => {
              const selected = selectedIds.includes(achievement.id);

              return (
                <button
                  key={achievement.id}
                  type="button"
                  disabled={isSaving}
                  className={styles.row}
                  onClick={() => handleSelect(achievement.id)}
                >
                  <span
                    className={
                      selected
                        ? `${styles.checkbox} ${styles.checkboxSelected}`
                        : styles.checkbox
                    }
                  >
                    {selected ? "✓" : ""}
                  </span>

                  <span className={styles.rowContent}>
                    <span className={styles.name}>{achievement.name}</span>

                    {achievement.description && (
                      <span className={styles.description}>
                        {achievement.description}
                      </span>
                    )}
                  </span>
                </button>
              );
            })}
          </div>

          <div className={styles.info}>


            <span>{"Точное количество дополнительных баллов покажем отдельно для каждого вуза."}</span>
          </div>
        </>
      )}
    </OnboardingLayout>
  );
}
