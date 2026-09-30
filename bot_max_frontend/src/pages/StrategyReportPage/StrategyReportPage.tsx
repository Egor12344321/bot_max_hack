import { useEffect, useState } from "react";

import { Button, Typography } from "@maxhub/max-ui";

import { useNavigate } from "react-router-dom";

import { useTranslation } from "react-i18next";

import type { StrategyFinalizeResponse } from "@/api/types/strategy";

import { getStrategyReport } from "@/api/strategyApi";

import { useAppSelector } from "@/store/hooks";

import styles from "./StrategyReportPage.module.css";

export function StrategyReportPage() {
  const { t } = useTranslation();

  const navigate = useNavigate();

  const sessionId = useAppSelector((state) => state.session.sessionId);

  const [report, setReport] = useState<StrategyFinalizeResponse | null>(null);

  const [isLoading, setIsLoading] = useState(true);

  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    async function loadReport() {
      if (!sessionId) {
        return;
      }

      try {
        setIsLoading(true);
        setError(null);

        const data = await getStrategyReport(sessionId);

        const sortedSummary = [...data.summary].sort(
          (a, b) => a.priority - b.priority,
        );

        setReport({
          ...data,
          summary: sortedSummary,
        });
      } catch {
        setError(t("strategyReport.loadError"));
      } finally {
        setIsLoading(false);
      }
    }

    loadReport();
  }, [sessionId, t]);

  function getStatusText(status: "reserve" | "real" | "risk") {
    if (status === "reserve") {
      return t("strategyReport.status.reserve");
    }

    if (status === "real") {
      return t("strategyReport.status.real");
    }

    return t("strategyReport.status.risk");
  }

  if (isLoading) {
    return (
      <div className={styles.page}>
        <div className={styles.state}>
          <Typography.Body>{t("common.loading")}</Typography.Body>
        </div>
      </div>
    );
  }

  if (error || !report) {
    return (
      <div className={styles.page}>
        <div className={styles.state}>
          <Typography.Body>{error ?? t("common.error")}</Typography.Body>

          <Button onClick={() => navigate("/priorities")}>
            {t("common.back")}
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div className={styles.page}>
      <main className={styles.content}>
        <section className={styles.success}>
          <div className={styles.successIcon}>✓</div>

          <div className={styles.successTitle}>{t("strategyReport.title")}</div>

          <div className={styles.successDescription}>
            {t("strategyReport.description")}
          </div>
        </section>

        <section>
          <div className={styles.sectionTitle}>
            {t("strategyReport.universities")}
          </div>

          <div className={styles.list}>
            {report.summary.map((item, index) => (
              <div key={item.universityId} className={styles.card}>
                <div className={styles.priority}>{index + 1}</div>

                <div className={styles.info}>
                  <div className={styles.name}>{item.universityName}</div>

                  <div className={styles.meta}>
                    <span className={`${styles.status} ${styles[item.status]}`}>
                      {getStatusText(item.status)}
                    </span>

                    <span>{item.chancePercent}%</span>
                  </div>

                  <div className={styles.scores}>
                    <span>
                      {t("strategyReport.myScore")}:{" "}
                      <strong>{item.myScore}</strong>
                    </span>

                    <span>·</span>

                    <span>
                      {t("strategyReport.passingScore")}:{" "}
                      <strong>{item.passingScorePreviousYear}</strong>
                    </span>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </section>

        <div className={styles.actions}>
          {report.reportUrl && (
            <Button
              stretched
              onClick={() => {
                window.open(report.reportUrl!, "_blank");
              }}
            >
              {t("strategyReport.openReport")}
            </Button>
          )}

          <Button
            stretched
            variant="secondary"
            onClick={() => navigate("/priorities")}
          >
            {t("strategyReport.edit")}
          </Button>

          <Button
            stretched
            variant="secondary"
            onClick={() => navigate("/profile")}
          >
            {t("strategyReport.toProfile")}
          </Button>
        </div>

        <div className={styles.botNote}>{t("strategyReport.botNote")}</div>
      </main>
    </div>
  );
}
