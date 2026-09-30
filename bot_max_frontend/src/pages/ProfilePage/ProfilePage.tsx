import { useCallback } from "react";

import { Button, Container } from "@maxhub/max-ui";

import { useNavigate } from "react-router-dom";

import type { Profile } from "@/api/types/profile";
import type { SavedDiploma } from "@/api/types/planning";

import { getProfile } from "@/api/profileApi";

import { AppLayout } from "@/components/AppLayout/AppLayout";
import { ApiError } from "@/components/Planning/ApiError";
import ui from "@/components/Planning/Planning.module.css";

import { useRemote } from "@/hooks/useRemote";
import { getMaxUserName } from "@/lib/max/user";
import { useAppSelector } from "@/store/hooks";

import styles from "./ProfilePage.module.css";

function diplomaLabel(diploma: SavedDiploma) {
  const level = diploma.level === null ? "ВсОШ" : `${diploma.level} уровень`;
  const degree = diploma.degree === "winner" ? "победитель" : "призёр";
  return `${diploma.profileName}, ${level}, ${degree}`;
}

function ProfileContent({ profile }: { profile: Profile }) {
  const navigate = useNavigate();
  const name = getMaxUserName() ?? "Абитуриент";
  const { programs } = profile;

  return (
    <Container className={styles.page}>
      <section className={styles.profileCard}>
        <div className={styles.profileTop}>
          <div className={styles.avatar} aria-hidden="true">
            {name.slice(0, 1).toLocaleUpperCase("ru-RU")}
          </div>
          <div>
            <div className={styles.name}>{name}</div>
          </div>
        </div>
        <div className={styles.scoreRow}>
          <div>
            <div className={styles.scoreLabel}>Сумма баллов ЕГЭ</div>
            <div className={styles.scoreValue}>
              <span className={styles.score}>{profile.egeTotal}</span>
              {profile.egeScores.length > 0 && (
                <span className={styles.scoreMax}>
                  /{profile.egeScores.length * 100}
                </span>
              )}
            </div>
          </div>
        </div>
      </section>

      <section className={`${ui.card} ${styles.section}`}>
        <div className={ui.row}>
          <strong>Баллы ЕГЭ</strong>
          <button
            type="button"
            className={ui.button}
            onClick={() => navigate("/profile/ege")}
          >
            Изменить
          </button>
        </div>
        {!profile.egeScores.length && (
          <span className={ui.muted}>Баллы ЕГЭ пока не указаны.</span>
        )}
        {profile.egeScores.map((item) => (
          <div key={item.subjectId} className={ui.row}>
            <span>{item.subjectName}</span>
            <span>
              <strong>{item.score}</strong>
              {!item.passed && (
                <span className={ui.muted}>
                  {" "}
                  · ниже порога {item.minThreshold}
                </span>
              )}
            </span>
          </div>
        ))}
      </section>

      <section className={styles.section}>
        <div className={styles.sectionTitle}>
          Подборка по выбранным направлениям
        </div>
        <div className={ui.muted}>
          Программ в подборке: {programs.total}. Сравнение с проходным баллом
          прошлого года, это не гарантия поступления.
        </div>
      </section>

      <section className={styles.advice}>
        <div>
          <div className={styles.adviceTitle}>Совет</div>
          <div className={styles.adviceText}>{profile.advice}</div>
        </div>
      </section>

      <section className={`${ui.card} ${styles.section}`}>
        <strong>Направления</strong>
        {!profile.directions.length && (
          <span className={ui.muted}>Направления не выбраны.</span>
        )}
        <div className={ui.actions}>
          {profile.directions.map((item) => (
            <span key={item.id} className={ui.badge}>
              {item.code} · {item.name}
            </span>
          ))}
        </div>
      </section>

      {(profile.olympiads.length > 0 ||
        profile.achievements.length > 0 ||
        profile.privileges.length > 0) && (
        <section className={`${ui.card} ${styles.section}`}>
          {profile.olympiads.length > 0 && (
            <>
              <strong>Олимпиады</strong>
              {profile.olympiads.map((item) => (
                <div key={item.profileId}>
                  <div>{item.olympiadName}</div>
                  <div className={ui.muted}>{diplomaLabel(item)}</div>
                </div>
              ))}
            </>
          )}
          {profile.achievements.length > 0 && (
            <>
              <strong>Достижения</strong>
              <div className={ui.actions}>
                {profile.achievements.map((item) => (
                  <span key={item} className={ui.badge}>
                    {item}
                  </span>
                ))}
              </div>
            </>
          )}
          {profile.privileges.length > 0 && (
            <>
              <strong>Льготы</strong>
              <div className={ui.actions}>
                {profile.privileges.map((item) => (
                  <span key={item} className={ui.badge}>
                    {item}
                  </span>
                ))}
              </div>
            </>
          )}
        </section>
      )}

      <Button stretched onClick={() => navigate("/universities")}>
        Открыть подборку
      </Button>
    </Container>
  );
}

export function ProfilePage() {
  const sessionId = useAppSelector((state) => state.session.sessionId)!;
  const loader = useCallback(() => getProfile(sessionId), [sessionId]);
  const { data, error, retry } = useRemote(loader);

  return (
    <AppLayout>
      {data ? (
        <ProfileContent profile={data} />
      ) : (
        <Container className={styles.page}>
          {error !== undefined ? (
            <ApiError error={error} retry={retry} />
          ) : (
            <div className={styles.state}>Загрузка...</div>
          )}
        </Container>
      )}
    </AppLayout>
  );
}
