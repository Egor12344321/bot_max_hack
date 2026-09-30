import { Button, Container, Panel, Typography } from "@maxhub/max-ui";
import type { ErrorDetails } from "@/utils/appError";
import styles from "./ErrorPage.module.css";

interface ErrorPageProps {
  error: ErrorDetails;
  onRetry: () => void;
}

export function ErrorPage({ error, onRetry }: ErrorPageProps) {
const hint = error.code === "MAX_SDK_UNAVAILABLE"
    ? "SDK MAX недоступен. Открой приложение через бота в MAX."
    : error.code === "NETWORK_ERROR"
      ? "Не удалось получить ответ сервера. Проверь подключение и попробуй ещё раз."
      : "Не удалось завершить запуск. Попробуй ещё раз. Если ошибка повторится, сообщи код и статус.";

  return (
    <Panel mode="secondary" className={styles.page}>
      <Container>
        <div className={styles.content} role="alert">
          <Typography.Title>{"Не удалось открыть приложение"}</Typography.Title>
          <Typography.Body>{hint}</Typography.Body>
          <div className={styles.details}>
            <Typography.Body>
              {`HTTP-статус: ${error.status ?? "нет ответа сервера"}`}
            </Typography.Body>
            <Typography.Body>{`Код ошибки: ${error.code}`}</Typography.Body>
          </div>
          <Button onClick={onRetry}>{"Повторить"}</Button>
        </div>
      </Container>
    </Panel>
  );
}
