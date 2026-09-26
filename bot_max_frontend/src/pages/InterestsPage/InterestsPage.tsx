import { useEffect, useState } from "react";

import { Typography } from "@maxhub/max-ui";

import { useNavigate } from "react-router-dom";

import { useTranslation } from "react-i18next";

import type { InterestCategory } from "@/api/types/onboarding";

import { getInterests, saveInterests } from "@/api/onboardingApi";

import { OnboardingLayout } from "@/components/OnboardingLayout/OnboardingLayout";

import { useAppSelector } from "@/store/hooks";
import { useOnboardingSelection } from "@/hooks/useOnboardingSelection";

import { getInterestIcon } from "@/utils/getInterestIcon";

import styles from "./InterestsPage.module.css";

export function InterestsPage() {
  const navigate = useNavigate();

  const { t } = useTranslation();

  const sessionId = useAppSelector((state) => state.session.sessionId);

  const [interests, setInterests] = useState<InterestCategory[]>([]);

  const [selectedIds, setSelectedIds] = useOnboardingSelection("selectedInterestIds");

  const [isLoading, setIsLoading] = useState(true);

  const [isSaving, setIsSaving] = useState(false);

  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    async function loadInterests() {
      try {
        setIsLoading(true);
        setError(null);

        const data = await getInterests();

        setInterests(data);
      } catch {
        setError(t("onboarding.interests.loadError"));
      } finally {
        setIsLoading(false);
      }
    }

    loadInterests();
  }, [t]);

  function handleInterestClick(interestId: string) {
    const isSelected = selectedIds.includes(interestId);

    if (isSelected) {
      setSelectedIds(selectedIds.filter((id) => id !== interestId));

      return;
    }

    if (selectedIds.length >= 3) {
      return;
    }

    setSelectedIds([...selectedIds, interestId]);
  }

  async function handleContinue() {
    if (!sessionId) {
      return;
    }

    try {
      setIsSaving(true);
      setError(null);

      await saveInterests(sessionId, selectedIds);

      navigate("/onboarding/achievements");
    } catch {
      setError(t("onboarding.interests.saveError"));
    } finally {
      setIsSaving(false);
    }
  }

  return (
    <OnboardingLayout
      step={1}
      totalSteps={3}
      title={t("onboarding.interests.title")}
      description={t("onboarding.interests.description")}
      buttonText={t("common.continue")}
      buttonDisabled={selectedIds.length === 0 || isLoading || interests.length === 0}
      buttonLoading={isSaving}
      onButtonClick={handleContinue}
    >
      {isLoading && (
        <div className={styles.state}>
          <Typography.Body>{t("common.loading")}</Typography.Body>
        </div>
      )}

      {error && (
        <div className={styles.error}>
          <Typography.Body>{error}</Typography.Body>
        </div>
      )}

      {!isLoading && (
        <>
          <div className={styles.grid}>
            {interests.map((interest) => {
              const isSelected = selectedIds.includes(interest.id);

              const icon = getInterestIcon(interest.id);

              return (
                <button
                  key={interest.id}
                  type="button"
                  className={
                    isSelected
                      ? `${styles.card} ${styles.cardSelected}`
                      : styles.card
                  }
                  onClick={() => handleInterestClick(interest.id)}
                >
                  {icon ? (
                    <img src={icon} alt="" className={styles.iconImage} />
                  ) : (
                    <span className={styles.icon}>{interest.icon}</span>
                  )}

                  <span className={styles.name}>{interest.name}</span>
                </button>
              );
            })}
          </div>

          <div className={styles.info}>
            <span>ℹ️</span>

            <Typography.Body>
              {t("onboarding.interests.selected", {
                count: selectedIds.length,
              })}
            </Typography.Body>
          </div>
        </>
      )}
    </OnboardingLayout>
  );
}
