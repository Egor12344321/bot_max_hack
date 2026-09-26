import { useEffect, useState } from "react";

import { Button, Container, Typography } from "@maxhub/max-ui";

import { useNavigate } from "react-router-dom";

import { useTranslation } from "react-i18next";

import type { ProfileSummary } from "@/api/types/profile";

import { getProfile } from "@/api/profileApi";
import { getEgeScoreSummary } from "@/utils/egeScore";
import { selectEgeScores } from "@/store/slices/sessionSlice";

import { AppLayout } from "@/components/AppLayout/AppLayout";

import { ChanceCircle } from "@/components/ChanceCircle/ChanceCircle";

import { useAppSelector } from "@/store/hooks";

import styles from "./ProfilePage.module.css";

export function ProfilePage() {
  const { t } = useTranslation();

  const navigate = useNavigate();

  const sessionId = useAppSelector((state) => state.session.sessionId);

  const [profile, setProfile] = useState<ProfileSummary | null>(null);
  const egeScores = useAppSelector(selectEgeScores);
  const egeScore = getEgeScoreSummary(egeScores);

  const [isLoading, setIsLoading] = useState(true);

  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    async function loadProfile() {
      if (!sessionId) {
        return;
      }

      try {
        setIsLoading(true);
        setError(null);

        const data = await getProfile(sessionId);

        setProfile(data);
      } catch {
        setError(t("profile.loadError"));
      } finally {
        setIsLoading(false);
      }
    }

    loadProfile();
  }, [sessionId, t]);

  if (isLoading) {
    return (
      <AppLayout>
        <div className={styles.state}>
          <Typography.Body>{t("common.loading")}</Typography.Body>
        </div>
      </AppLayout>
    );
  }

  if (error || !profile) {
    return (
      <AppLayout>
        <div className={styles.state}>
          <Typography.Body>{error || t("common.error")}</Typography.Body>
        </div>
      </AppLayout>
    );
  }

  return (
    <AppLayout>
      <Container className={styles.page}>
        <section className={styles.profileCard}>
          <div className={styles.profileTop}>
            <div className={styles.avatar}>🎓</div>

            <div>
              <div className={styles.name}>{profile.fullName}</div>

              <div className={styles.subtitle}>
                {t("profile.graduation", {
                  year: profile.graduationYear,
                })}

                {" · "}

                {profile.mainInterestCategory}
              </div>
            </div>
          </div>

          <div className={styles.scoreRow}>
            <div>
              <div className={styles.scoreLabel}>{t("profile.totalScore")}</div>

              <div className={styles.scoreValue}>
                <span className={styles.score}>{egeScore.total}</span>

                <span className={styles.scoreMax}>/{egeScore.max}</span>
              </div>
            </div>
          </div>
        </section>

        <section className={styles.chanceCard}>
          <ChanceCircle percent={profile.admissionProbabilityPercent} />

          <div>
            <div className={styles.chanceTitle}>{t("profile.chanceTitle")}</div>

            <div className={styles.chanceDescription}>
              {t("profile.scoreDifference", {
                count: profile.scoreDeltaVsAveragePassing,
              })}
            </div>
          </div>
        </section>

        <div className={styles.statusRow}>
          <div className={`${styles.statusCard} ${styles.reserve}`}>
            <div className={styles.statusEmoji}>🟢</div>

            <div className={styles.statusNumber}>{profile.reserveCount}</div>

            <div className={styles.statusName}>{t("profile.reserve")}</div>

            <div className={styles.statusDescription}>
              {t("profile.reserveDescription")}
            </div>
          </div>

          <div className={`${styles.statusCard} ${styles.real}`}>
            <div className={styles.statusEmoji}>🟡</div>

            <div className={styles.statusNumber}>{profile.realCount}</div>

            <div className={styles.statusName}>{t("profile.real")}</div>

            <div className={styles.statusDescription}>
              {t("profile.realDescription")}
            </div>
          </div>

          <div className={`${styles.statusCard} ${styles.risk}`}>
            <div className={styles.statusEmoji}>🔴</div>

            <div className={styles.statusNumber}>{profile.riskCount}</div>

            <div className={styles.statusName}>{t("profile.risk")}</div>

            <div className={styles.statusDescription}>
              {t("profile.riskDescription")}
            </div>
          </div>
        </div>

        <section className={styles.advice}>
          <span className={styles.adviceIcon}>💡</span>

          <div>
            <div className={styles.adviceTitle}>{t("profile.advice")}</div>

            <div className={styles.adviceText}>{profile.advice}</div>
          </div>
        </section>

        <Button stretched onClick={() => navigate("/priorities")}>
          {t("profile.openStrategy")}
        </Button>
      </Container>
    </AppLayout>
  );
}
