import { Button, Container, Panel, Typography } from "@maxhub/max-ui";

import { useTranslation } from "react-i18next";

import { AppRouter } from "@/app/router";

import { useAppSelector } from "@/store/hooks";

import { useAppInit } from "@/hooks/useAppInit";

import styles from "./App.module.css";

export function App() {
  const { retry } = useAppInit();

  const { t } = useTranslation();

  const isInitialized = useAppSelector((state) => state.session.isInitialized);
  const initializationError = useAppSelector((state) => state.session.initializationError);
  const isComplete = useAppSelector((state) => state.session.isCompleteFromBot);

  if (initializationError || (isInitialized && !isComplete)) {
    return (
      <Panel mode="secondary" className={styles.app}>
        <Container>
          <div className={styles.content} role="alert">
            <Typography.Title>{t(initializationError ? "common.initError" : "common.incompleteSession")}</Typography.Title>
            <Typography.Body>{t(initializationError ? "common.initErrorHint" : "common.completeInBot")}</Typography.Body>
            <Button onClick={retry}>{t("common.retry")}</Button>
          </div>
        </Container>
      </Panel>
    );
  }

  if (!isInitialized) {
    return (
      <Panel mode="secondary" className={styles.app}>
        <Container>
          <div className={styles.content}>
            <Typography.Body>{t("common.loading")}</Typography.Body>
          </div>
        </Container>
      </Panel>
    );
  }

  return <AppRouter />;
}
