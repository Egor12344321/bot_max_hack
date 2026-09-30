import { Button, Container, Panel, Typography } from "@maxhub/max-ui";

import { AppRouter } from "@/app/router";

import { useAppSelector } from "@/store/hooks";

import { useAppInit } from "@/hooks/useAppInit";
import { ErrorPage } from "@/pages/ErrorPage/ErrorPage";

import styles from "./App.module.css";

export function App() {
  const { retry } = useAppInit();

const isInitialized = useAppSelector((state) => state.session.isInitialized);
  const initializationError = useAppSelector((state) => state.session.initializationError);
  const errorDetails = useAppSelector((state) => state.session.initializationErrorDetails);
  const isComplete = useAppSelector((state) => state.session.isCompleteFromBot);

  if (initializationError) {
    return <ErrorPage error={errorDetails ?? { status: null, code: "UNKNOWN_ERROR" }} onRetry={retry} />;
  }

  if (isInitialized && !isComplete) {
    return (
      <Panel mode="secondary" className={styles.app}>
        <Container>
          <div className={styles.content} role="alert">
            <Typography.Title>{"Сначала заверши ввод данных"}</Typography.Title>
            <Typography.Body>{"Вернись в чат с ботом, укажи гражданство и баллы ЕГЭ, затем открой мини-приложение снова."}</Typography.Body>
            <Button onClick={retry}>{"Повторить"}</Button>
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
            <Typography.Body>{"Загрузка..."}</Typography.Body>
          </div>
        </Container>
      </Panel>
    );
  }

  return <AppRouter />;
}
