import { useEffect, useState } from "react";

import { Typography } from "@maxhub/max-ui";

import { useNavigate } from "react-router-dom";

import type { PrivilegeCategory } from "@/api/types/onboarding";

import { getPrivileges, savePrivileges } from "@/api/onboardingApi";

import { OnboardingLayout } from "@/components/OnboardingLayout/OnboardingLayout";

import { useAppSelector } from "@/store/hooks";
import { useOnboardingSelection } from "@/hooks/useOnboardingSelection";

import styles from "./PrivilegesPage.module.css";

export function PrivilegesPage() {
  const navigate = useNavigate();

const sessionId = useAppSelector((state) => state.session.sessionId);

  const [privileges, setPrivileges] = useState<PrivilegeCategory[]>([]);

  const [selectedIds, setSelectedIds] = useOnboardingSelection("selectedPrivilegeIds");

  const [isLoading, setIsLoading] = useState(true);
  const [loadFailed, setLoadFailed] = useState(false);

  const [isSaving, setIsSaving] = useState(false);

  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    async function loadPrivileges() {
      try {
        setIsLoading(true);
        setLoadFailed(false);
        setError(null);

        const data = await getPrivileges();

        setPrivileges(data);
      } catch {
        setLoadFailed(true);
        setError("Не удалось загрузить категории");
      } finally {
        setIsLoading(false);
      }
    }

    loadPrivileges();
  }, []);

  function handleSelect(privilegeId: string) {
    if (selectedIds.includes(privilegeId)) {
      setSelectedIds(selectedIds.filter((id) => id !== privilegeId));

      return;
    }

    setSelectedIds([...selectedIds, privilegeId]);
  }

  async function handleContinue() {
    if (!sessionId) {
      return;
    }

    try {
      setIsSaving(true);
      setError(null);

      await savePrivileges(sessionId, selectedIds);

      navigate("/universities");
    } catch {
      setError("Не удалось сохранить данные");
    } finally {
      setIsSaving(false);
    }
  }

  return (
    <OnboardingLayout
      step={4}
      totalSteps={4}
      title={"Есть основания для льготы?"}
      description={"Отметь всё, что относится к тебе. Подходящий вариант поступления определим автоматически."}
      buttonText={"Рассчитать шансы"}
      buttonLoading={isSaving}
      buttonDisabled={isLoading || loadFailed}
      onButtonClick={handleContinue}
      onBack={() => { if (!isSaving) navigate("/onboarding/achievements"); }}
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
              {"Особый статус"}
            </div>

            {privileges.map((privilege) => {
              const selected = selectedIds.includes(privilege.id);

              return (
                <button
                  key={privilege.id}
                  type="button"
                  disabled={isSaving}
                  className={styles.row}
                  onClick={() => handleSelect(privilege.id)}
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

                  <span className={styles.name}>{privilege.name}</span>
                </button>
              );
            })}
          </div>

          <div className={styles.info}>


            <span>
              {selectedIds.length === 0
                ? "Ничего не выбрано — можно продолжить без льгот"
                : `Выбрано: ${selectedIds.length}`}
            </span>
          </div>
        </>
      )}
    </OnboardingLayout>
  );
}
