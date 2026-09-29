import { useCallback, useState } from "react";
import { useNavigate } from "react-router-dom";
import { request } from "@/api/client";
import {
  getDirections,
  getSelectedDirections,
  saveSelectedDirections,
} from "@/api/planningApi";
import type { StudyDirection } from "@/api/types/planning";
import type { InterestCategory } from "@/api/types/onboarding";
import { OnboardingLayout } from "@/components/OnboardingLayout/OnboardingLayout";
import { ApiError } from "@/components/Planning/ApiError";
import { useAppSelector } from "@/store/hooks";
import { useRemote } from "@/hooks/useRemote";
import { resolveDirections } from "@/utils/directionCatalog";
import { moveItem } from "@/utils/applicationPlan";
import { getInterestIcon } from "@/utils/getInterestIcon";
import styles from "./InterestsPage.module.css";
import ui from "@/components/Planning/Planning.module.css";

function DirectionList({
  category,
  query,
  selected,
  onToggle,
}: {
  category: string;
  query: string;
  selected: StudyDirection[];
  onToggle: (direction: StudyDirection) => void;
}) {
  const [offset, setOffset] = useState(0);
  const loader = useCallback(
    () => getDirections(query, offset, category),
    [query, offset, category],
  );
  const { data, error, loading, retry } = useRemote(loader);
  return (
    <div id="direction-catalog" className={ui.card}>
      {loading && <p role="status">Загрузка направлений…</p>}
      {error !== undefined && <ApiError error={error} retry={retry} />}
      {data && (
        <>
          {data.items.length === 0 && (
            <p>
              Направления не найдены. Попробуйте другой запрос или категорию.
            </p>
          )}
          {data.items.map((item) => (
            <label className={ui.check} key={item.id}>
              <input
                type="checkbox"
                checked={selected.some((d) => d.id === item.id)}
                onChange={() => onToggle(item)}
              />
              <span>
                <strong>{item.name}</strong>
                <br />
                <span className={ui.muted}>{item.code}</span>
              </span>
            </label>
          ))}
          <div className={ui.row}>
            <button
              className={ui.button}
              disabled={!offset}
              onClick={() => setOffset(offset - 20)}
            >
              Назад
            </button>
            <span className={ui.muted}>
              {data.total ? offset + 1 : 0}–{offset + data.items.length} из{" "}
              {data.total}
            </span>
            <button
              className={ui.button}
              disabled={offset + data.items.length >= data.total}
              onClick={() => setOffset(offset + 20)}
            >
              Далее
            </button>
          </div>
        </>
      )}
    </div>
  );
}
function DirectionForm({
  sessionId,
  categories,
  initial,
}: {
  sessionId: string;
  categories: InterestCategory[];
  initial: StudyDirection[];
}) {
  const navigate = useNavigate();
  const [selected, setSelected] = useState(initial);
  const [category, setCategory] = useState<string | null>(null);
  const [query, setQuery] = useState("");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<unknown>();
  async function save() {
    setSaving(true);
    setError(undefined);
    try {
      await saveSelectedDirections(
        sessionId,
        selected.map((item) => item.id),
      );
      navigate("/onboarding/olympiads");
    } catch (e) {
      setError(e);
    } finally {
      setSaving(false);
    }
  }
  return (
    <OnboardingLayout
      step={1}
      totalSteps={3}
      title="Что вам интересно?"
      description="Откройте область интересов и выберите направления обучения. Можно выбрать несколько областей и любое количество направлений."
      buttonText="К олимпиадам"
      buttonLoading={saving}
      onButtonClick={save}
    >
      <fieldset
        disabled={saving}
        style={{ border: 0, padding: 0, margin: 0, minWidth: 0 }}
        className={ui.stack}
      >
        {error !== undefined && <ApiError error={error} />}
        <div className={styles.grid}>
          {categories.map((item) => {
            const icon = getInterestIcon(item.id);
            return (
              <button
                key={item.id}
                className={`${styles.card} ${category === item.id ? styles.cardSelected : ""}`}
                aria-expanded={category === item.id}
                aria-controls="direction-catalog"
                onClick={() => {
                  setCategory(item.id);
                  setQuery("");
                }}
              >
                {icon ? (
                  <img src={icon} alt="" className={styles.iconImage} />
                ) : (
                  <span className={styles.icon}>{item.icon}</span>
                )}
                <span className={styles.name}>{item.name}</span>
              </button>
            );
          })}
        </div>
        <button
          className={ui.button}
          onClick={() => {
            setCategory("");
            setQuery("");
          }}
        >
          Все направления
        </button>
        {category !== null && (
          <>
            <h2>
              {categories.find((item) => item.id === category)?.name ??
                "Все направления"}
            </h2>
            <input
              className={ui.input}
              aria-label="Поиск направлений"
              placeholder="Название или код направления"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
            />
            <DirectionList
              key={`${category}:${query}`}
              category={category}
              query={query}
              selected={selected}
              onToggle={(item) =>
                setSelected((current) =>
                  current.some((d) => d.id === item.id)
                    ? current.filter((d) => d.id !== item.id)
                    : [...current, item],
                )
              }
            />
          </>
        )}
        <section className={ui.card}>
          <h2>Выбрано направлений: {selected.length}</h2>
          {!selected.length && (
            <p className={ui.muted}>
              Выбор пока пуст. Вы сможете вернуться к нему позже.
            </p>
          )}
          {selected.map((item, index) => (
            <div key={item.id} className={ui.stack}>
              <span>
                {index + 1}. {item.code} {item.name}
              </span>
              <div className={ui.actions}>
                <button
                  className={ui.button}
                  aria-label={`Поднять ${item.name}`}
                  disabled={!index}
                  onClick={() => setSelected(moveItem(selected, index, -1))}
                >
                  ↑
                </button>
                <button
                  className={ui.button}
                  aria-label={`Опустить ${item.name}`}
                  disabled={index === selected.length - 1}
                  onClick={() => setSelected(moveItem(selected, index, 1))}
                >
                  ↓
                </button>
                <button
                  className={ui.button}
                  onClick={() =>
                    setSelected(selected.filter((d) => d.id !== item.id))
                  }
                >
                  Убрать
                </button>
              </div>
            </div>
          ))}
        </section>
      </fieldset>
    </OnboardingLayout>
  );
}
export function InterestsPage() {
  const sessionId = useAppSelector((s) => s.session.sessionId)!;
  const loader = useCallback(async () => {
    const [categories, selection] = await Promise.all([
      request<InterestCategory[]>("/interest-categories"),
      getSelectedDirections(sessionId),
    ]);
    return {
      categories,
      initial: await resolveDirections(selection.directionIds),
    };
  }, [sessionId]);
  const { data, error, retry } = useRemote(loader);
  if (data)
    return <DirectionForm key={sessionId} sessionId={sessionId} {...data} />;
  return (
    <OnboardingLayout
      step={1}
      totalSteps={3}
      title="Направления обучения"
      buttonText="Далее"
      buttonDisabled
      onButtonClick={() => {}}
    >
      {error !== undefined ? (
        <ApiError error={error} retry={retry} />
      ) : (
        <p role="status">Загрузка…</p>
      )}
    </OnboardingLayout>
  );
}
