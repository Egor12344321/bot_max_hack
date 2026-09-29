import { useCallback, useState } from "react";

import { Button, Container } from "@maxhub/max-ui";

import { useNavigate } from "react-router-dom";

import type { EgeScoreResult, Subject } from "@/api/types/session";

import { getEgeScores, getSubjects, saveEgeScores } from "@/api/egeApi";

import { AppLayout } from "@/components/AppLayout/AppLayout";
import { ApiError } from "@/components/Planning/ApiError";
import ui from "@/components/Planning/Planning.module.css";

import { useRemote } from "@/hooks/useRemote";
import { useAppDispatch, useAppSelector } from "@/store/hooks";
import { setEgeScores } from "@/store/slices/sessionSlice";

interface Row {
  subjectId: string;
  name: string;
  minThreshold: number | null;
}

/** Предметы справочника плюс те, что уже сохранены, но в справочнике не нашлись. */
function rowsOf(subjects: Subject[], scores: EgeScoreResult[]): Row[] {
  const rows: Row[] = subjects.map((subject) => ({
    subjectId: subject.id,
    name: subject.name,
    minThreshold: subject.minThreshold,
  }));
  for (const score of scores) {
    if (!rows.some((row) => row.subjectId === score.subjectId)) {
      rows.push({ subjectId: score.subjectId, name: score.subjectName, minThreshold: score.minThreshold || null });
    }
  }
  return rows;
}

function parseScore(value: string): number | null | "invalid" {
  if (!value.trim()) return null;
  if (!/^\d{1,3}$/.test(value.trim())) return "invalid";
  const score = Number(value.trim());
  return score <= 100 ? score : "invalid";
}

function EgeForm({ subjects, scores }: { subjects: Subject[]; scores: EgeScoreResult[] }) {
  const navigate = useNavigate();
  const dispatch = useAppDispatch();
  const sessionId = useAppSelector((state) => state.session.sessionId)!;
  const rows = rowsOf(subjects, scores);
  const [values, setValues] = useState<Record<string, string>>(() =>
    Object.fromEntries(scores.map((score) => [score.subjectId, String(score.score)])),
  );
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<unknown>();

  const parsed = rows.map((row) => ({ row, score: parseScore(values[row.subjectId] ?? "") }));
  const invalid = parsed.filter((item) => item.score === "invalid").map((item) => item.row.name);
  const filled = parsed.filter(
    (item): item is { row: Row; score: number } => typeof item.score === "number",
  );

  async function save() {
    setSaving(true);
    setError(undefined);
    try {
      const result = await saveEgeScores(
        sessionId,
        filled.map((item) => ({ subjectId: item.row.subjectId, score: item.score })),
      );
      dispatch(setEgeScores(result.scores));
      navigate("/profile");
    } catch (e) {
      setError(e);
    } finally {
      setSaving(false);
    }
  }

  return (
    <Container className={ui.page}>
      <div className={ui.title}>Баллы ЕГЭ</div>
      <p className={ui.muted}>
        Укажите баллы по сданным предметам, пустые поля не сохраняются. Подборка вузов пересчитается сразу после
        сохранения.
      </p>
      {error !== undefined && <ApiError error={error} />}
      <fieldset disabled={saving} className={ui.stack} style={{ border: 0, padding: 0, margin: 0, minWidth: 0 }}>
        {rows.map((row) => {
          const value = values[row.subjectId] ?? "";
          const score = parseScore(value);
          const below = typeof score === "number" && row.minThreshold !== null && score < row.minThreshold;
          return (
            <label key={row.subjectId} className={ui.label}>
              <span>
                {row.name}
                {row.minThreshold !== null && <span className={ui.muted}> · минимум {row.minThreshold}</span>}
              </span>
              <input
                className={ui.input}
                inputMode="numeric"
                aria-invalid={score === "invalid"}
                placeholder="Не сдавал(а)"
                value={value}
                onChange={(e) => setValues((current) => ({ ...current, [row.subjectId]: e.target.value }))}
              />
              {score === "invalid" && <span className={ui.muted}>Введите целое число от 0 до 100.</span>}
              {below && <span className={ui.muted}>Ниже минимального порога: вузы такой результат не примут.</span>}
            </label>
          );
        })}
      </fieldset>
      {invalid.length > 0 && <div className={ui.error}>Исправьте баллы: {invalid.join(", ")}.</div>}
      <div className={ui.actions}>
        <Button stretched loading={saving} disabled={invalid.length > 0} onClick={save}>
          Сохранить
        </Button>
        <button type="button" className={ui.button} disabled={saving} onClick={() => navigate("/profile")}>
          Отмена
        </button>
      </div>
    </Container>
  );
}

export function EgeScoresPage() {
  const sessionId = useAppSelector((state) => state.session.sessionId)!;
  const loader = useCallback(async () => {
    const [subjects, current] = await Promise.all([getSubjects(), getEgeScores(sessionId)]);
    return { subjects, scores: current.scores };
  }, [sessionId]);
  const { data, error, retry } = useRemote(loader);

  return (
    <AppLayout>
      {data ? (
        <EgeForm subjects={data.subjects} scores={data.scores} />
      ) : (
        <Container className={ui.page}>
          {error !== undefined ? <ApiError error={error} retry={retry} /> : <p>Загрузка...</p>}
        </Container>
      )}
    </AppLayout>
  );
}
