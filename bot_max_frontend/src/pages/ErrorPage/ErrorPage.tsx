import { Button, Container, Panel, Typography } from "@maxhub/max-ui";
import { useTranslation } from "react-i18next";
import type { ErrorDetails } from "@/utils/appError";
import styles from "./ErrorPage.module.css";

interface ErrorPageProps {
  error: ErrorDetails;
  onRetry: () => void;
}

export function ErrorPage({ error, onRetry }: ErrorPageProps) {
  const { t } = useTranslation();
  const hint = error.code === "MAX_SDK_UNAVAILABLE"
    ? "errors.page.maxUnavailable"
    : error.code === "NETWORK_ERROR"
      ? "errors.page.network"
      : "errors.page.hint";

  return (
    <Panel mode="secondary" className={styles.page}>
      <Container>
        <div className={styles.content} role="alert">
          <Typography.Title>{t("common.initError")}</Typography.Title>
          <Typography.Body>{t(hint)}</Typography.Body>
          <div className={styles.details}>
            <Typography.Body>
              {t("errors.page.status", { status: error.status ?? t("errors.page.noStatus") })}
            </Typography.Body>
            <Typography.Body>{t("errors.page.code", { code: error.code })}</Typography.Body>
          </div>
          <Button onClick={onRetry}>{t("common.retry")}</Button>
        </div>
      </Container>
    </Panel>
  );
}
