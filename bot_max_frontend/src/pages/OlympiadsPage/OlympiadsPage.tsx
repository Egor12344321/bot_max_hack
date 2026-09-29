import { useCallback, useState } from "react";
import { useNavigate } from "react-router-dom";
import { getDiplomas, getOlympiads, saveDiplomas } from "@/api/planningApi";
import type { Diploma, Olympiad, SavedDiploma } from "@/api/types/planning";
import { OnboardingLayout } from "@/components/OnboardingLayout/OnboardingLayout";
import { ApiError } from "@/components/Planning/ApiError";
import { useRemote } from "@/hooks/useRemote";
import { useAppSelector } from "@/store/hooks";
import ui from "@/components/Planning/Planning.module.css";

function DiplomaForm({
  sessionId,
  catalog,
  initial,
}: {
  sessionId: string;
  catalog: Olympiad[];
  initial: SavedDiploma[];
}) {
  const navigate = useNavigate();
  const [diplomas, setDiplomas] = useState<Diploma[]>(initial);
  const [query, setQuery] = useState("");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<unknown>();
  const profiles = catalog.flatMap((olympiad) =>
    olympiad.profiles.map((p) => ({ ...p, olympiadName: olympiad.name })),
  );
  const all = [
    ...profiles,
    ...initial
      .filter((d) => !profiles.some((p) => p.id === d.profileId))
      .map((d) => ({
        id: d.profileId,
        name: d.profileName,
        year: d.year,
        level: d.level,
        olympiadName: d.olympiadName,
      })),
  ];
  const visible = all.filter((p) =>
    `${p.olympiadName} ${p.name} ${p.year}`
      .toLocaleLowerCase()
      .includes(query.toLocaleLowerCase()),
  );
  function select(id: string, value: string) {
    setDiplomas((current) => [
      ...current.filter((d) => d.profileId !== id),
      ...(value ? [{ profileId: id, degree: value as Diploma["degree"] }] : []),
    ]);
  }
  async function save() {
    setSaving(true);
    setError(undefined);
    try {
      await saveDiplomas(sessionId, diplomas);
      navigate("/onboarding/achievements");
    } catch (e) {
      setError(e);
    } finally {
      setSaving(false);
    }
  }
  return (
    <OnboardingLayout
      step={2}
      totalSteps={3}
      title="Олимпиады"
      description="Укажите профиль, год диплома и результат. Если дипломов нет, переходите дальше. Право на льготы проверяется отдельно для каждой программы."
      buttonText="К индивидуальным достижениям"
      buttonLoading={saving}
      onButtonClick={save}
      onBack={() => {
        if (!saving) navigate("/onboarding/interests");
      }}
    >
      <fieldset
        disabled={saving}
        className={ui.stack}
        style={{ border: 0, padding: 0, margin: 0, minWidth: 0 }}
      >
        {error !== undefined && <ApiError error={error} />}
        <p>Выбрано дипломов: {diplomas.length}</p>
        <input
          className={ui.input}
          aria-label="Поиск олимпиад"
          placeholder="Олимпиада, профиль или год"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
        />
        {!visible.length && <p>Олимпиады не найдены.</p>}
        {visible.map((p) => (
          <article key={p.id} className={ui.card}>
            <strong>{p.olympiadName}</strong>
            <span>{p.name}</span>
            <span className={ui.muted}>
              {p.year} год{p.level !== null ? ` · Уровень ${p.level}` : ""}
            </span>
            <label className={ui.label}>
              Результат
              <select
                className={ui.input}
                value={diplomas.find((d) => d.profileId === p.id)?.degree ?? ""}
                onChange={(e) => select(p.id, e.target.value)}
              >
                <option value="">Нет диплома</option>
                <option value="winner">Победитель</option>
                <option value="prize">Призёр</option>
              </select>
            </label>
          </article>
        ))}
      </fieldset>
    </OnboardingLayout>
  );
}
export function OlympiadsPage() {
  const sessionId = useAppSelector((s) => s.session.sessionId)!;
  const navigate = useNavigate();
  const loader = useCallback(async () => {
    const [catalog, initial] = await Promise.all([
      getOlympiads(),
      getDiplomas(sessionId),
    ]);
    return { catalog, initial };
  }, [sessionId]);
  const { data, error, retry } = useRemote(loader);
  if (data)
    return <DiplomaForm key={sessionId} sessionId={sessionId} {...data} />;
  return (
    <OnboardingLayout
      step={2}
      totalSteps={3}
      title="Олимпиады"
      buttonText="Далее"
      buttonDisabled
      onButtonClick={() => {}}
      onBack={() => navigate("/onboarding/interests")}
    >
      {error !== undefined ? (
        <ApiError error={error} retry={retry} />
      ) : (
        <p role="status">Загрузка…</p>
      )}
    </OnboardingLayout>
  );
}
