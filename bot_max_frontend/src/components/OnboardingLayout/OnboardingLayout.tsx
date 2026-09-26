import type { ReactNode } from "react";

import { Button, Container, Panel, Typography } from "@maxhub/max-ui";

import { useTranslation } from "react-i18next";

import styles from "./OnboardingLayout.module.css";

interface OnboardingLayoutProps {
  step: number;
  totalSteps: number;

  title: string;
  description?: string;

  children: ReactNode;

  buttonText: string;
  buttonDisabled?: boolean;
  buttonLoading?: boolean;

  onButtonClick: () => void;

  onBack?: () => void;
}

export function OnboardingLayout({
  step,
  totalSteps,
  title,
  description,
  children,
  buttonText,
  buttonDisabled = false,
  buttonLoading = false,
  onButtonClick,
  onBack,
}: OnboardingLayoutProps) {
  const { t } = useTranslation();

  return (
    <Panel mode="secondary" className={styles.page}>
      <header className={styles.header}>
        <div className={styles.headerInner}>
          <div className={styles.headerTop}>
            <Typography.Body>
              {t("common.step", {
                step,
                total: totalSteps,
              })}
            </Typography.Body>

            {onBack && (
              <button
                type="button"
                className={styles.backButton}
                onClick={onBack}
              >
                ← {t("common.back")}
              </button>
            )}
          </div>

          <div className={styles.progress}>
            {Array.from({
              length: totalSteps,
            }).map((_, index) => {
              const currentStep = index + 1;

              return (
                <div
                  key={currentStep}
                  className={
                    currentStep <= step
                      ? `${styles.progressItem} ${styles.progressItemActive}`
                      : styles.progressItem
                  }
                />
              );
            })}
          </div>
        </div>
      </header>

      <Container className={styles.content}>
        <div className={styles.intro}>
          <Typography.Title>{title}</Typography.Title>

          {description && <Typography.Body>{description}</Typography.Body>}
        </div>

        {children}
      </Container>

      <footer className={styles.footer}>
        <div className={styles.footerInner}>
          <Button
            stretched
            disabled={buttonDisabled || buttonLoading}
            onClick={onButtonClick}
          >
            {buttonLoading ? t("common.saving") : buttonText}
          </Button>
        </div>
      </footer>
    </Panel>
  );
}
