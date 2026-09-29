import { useCallback, useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import {
  getApplicationPlan,
  getRecommendations,
  getSelectedDirections,
  saveApplicationPlan,
} from "@/api/planningApi";
import type {
  ApplicationPlan,
  PlanComposition,
  ProgramOption,
  StudyDirection,
} from "@/api/types/planning";
import { AppLayout } from "@/components/AppLayout/AppLayout";
import { ApiError } from "@/components/Planning/ApiError";
import { ProgramCard } from "@/components/Planning/ProgramCard";
import { useRemote } from "@/hooks/useRemote";
import { useAppSelector } from "@/store/hooks";
import {
  addProgram,
  emptyComposition,
  moveItem,
  removeProgram,
  validateComposition,
} from "@/utils/applicationPlan";
import {
  clearPlanDraft,
  planToDraft,
  readPlanDraft,
  writePlanDraft,
  type PlanDraft,
} from "@/utils/planDraft";
import { getErrorDetails } from "@/utils/appError";
import ui from "@/components/Planning/Planning.module.css";

function RecommendationGroup({
  direction,
  sessionId,
  draft,
  disabled,
  onAdd,
}: {
  direction: StudyDirection;
  sessionId: string;
  draft: PlanDraft;
  disabled: boolean;
  onAdd: (p: ProgramOption) => void;
}) {
  const [offset, setOffset] = useState(0);
  const loader = useCallback(
    () => getRecommendations(sessionId, direction.id, offset),
    [sessionId, direction.id, offset],
  );
  const { data, error, loading, retry } = useRemote(loader);
  return (
    <section className={ui.stack}>
      <h2>
        {data?.items[0]?.direction.code ?? direction.code}{" "}
        {data?.items[0]?.direction.name ?? direction.name}
      </h2>
      {loading && <p role="status">Подбираем варианты…</p>}
      {error !== undefined && <ApiError error={error} retry={retry} />}
      {data && (
        <>
          {!data.items.length && (
            <div className={ui.notice}>
              Для этого направления варианты не найдены.
            </div>
          )}
          {data.items.map((p) => {
            const added = draft.composition.universities.some((u) =>
              u.programIds.includes(p.programId),
            );
            let blocked: string | null = null;
            try {
              addProgram(draft.composition, p, draft.options);
            } catch (e) {
              blocked =
                e instanceof Error ? e.message : "Нельзя добавить программу";
            }
            return (
              <ProgramCard key={p.programId} option={p}>
                <button
                  className={ui.button}
                  disabled={disabled || added || !!blocked}
                  onClick={() => onAdd(p)}
                >
                  {added ? "В плане" : "Добавить в план"}
                </button>
                {!added && blocked && <p className={ui.muted}>{blocked}</p>}
              </ProgramCard>
            );
          })}
          <div className={ui.row}>
            <button
              className={ui.button}
              disabled={!offset}
              onClick={() => setOffset(offset - 20)}
            >
              Назад
            </button>
            <span>
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
    </section>
  );
}

function PlanWorkspace({
  sessionId,
  initial,
  directions,
  directionsReady,
}: {
  sessionId: string;
  initial: ApplicationPlan;
  directions: StudyDirection[];
  directionsReady: boolean;
}) {
  const [params, setParams] = useSearchParams();
  const showPlan = params.get("view") === "plan";
  const [saved, setSaved] = useState(initial);
  const [draft, setDraft] = useState<PlanDraft>(() => {
    const cached = readPlanDraft(sessionId);
    if (!cached) return planToDraft(initial);
    const options = new Map(cached.options.map((p) => [p.programId, p]));
    initial.options.forEach((p) => options.set(p.programId, p));
    return { ...cached, options: [...options.values()] };
  });
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<unknown>();
  const [conflict, setConflict] = useState(
    draft.expectedVersion !== initial.version,
  );
  const [latest, setLatest] = useState<ApplicationPlan | null>(
    draft.expectedVersion !== initial.version ? initial : null,
  );
  const [notice, setNotice] = useState("");
  const dirty =
    JSON.stringify(draft.composition) !== JSON.stringify(saved.composition);
  useEffect(() => {
    if (!dirty) return;
    const warn = (event: BeforeUnloadEvent) => {
      event.preventDefault();
      event.returnValue = "";
    };
    window.addEventListener("beforeunload", warn);
    return () => window.removeEventListener("beforeunload", warn);
  }, [dirty]);
  function change(composition: PlanComposition, options = draft.options) {
    const next = { ...draft, composition, options };
    setDraft(next);
    setNotice("");
    setError(undefined);
    if (!writePlanDraft(sessionId, next))
      setNotice(
        "Черновик хранится только на этом экране: браузер запретил локальное сохранение.",
      );
  }
  function add(p: ProgramOption) {
    try {
      const options = [
        ...draft.options.filter((item) => item.programId !== p.programId),
        p,
      ];
      change(addProgram(draft.composition, p, options), options);
      setNotice("Программа добавлена в черновик. Сохраните итоговый план.");
    } catch (e) {
      setError(e);
    }
  }
  async function save() {
    const invalid = validateComposition(draft.composition, draft.options);
    if (invalid) {
      setError(new Error(invalid));
      return;
    }
    setBusy(true);
    setError(undefined);
    setNotice("");
    try {
      const plan = await saveApplicationPlan(
        sessionId,
        draft.composition,
        draft.expectedVersion,
      );
      setSaved(plan);
      setDraft(planToDraft(plan));
      clearPlanDraft(sessionId);
      setNotice("План сохранён.");
    } catch (e) {
      if (getErrorDetails(e).status === 409) {
        setConflict(true);
        setLatest(null);
      } else setError(e);
    } finally {
      setBusy(false);
    }
  }
  async function refreshConflict() {
    setBusy(true);
    setError(undefined);
    try {
      setLatest(await getApplicationPlan(sessionId));
    } catch (e) {
      setError(e);
    } finally {
      setBusy(false);
    }
  }
  function resolveConflict(keepLocal: boolean) {
    if (!latest) return;
    const options = new Map(draft.options.map((p) => [p.programId, p]));
    latest.options.forEach((p) => options.set(p.programId, p));
    const next = keepLocal
      ? {
          ...draft,
          expectedVersion: latest.version,
          options: [...options.values()],
        }
      : planToDraft(latest);
    setSaved(latest);
    setDraft(next);
    setConflict(false);
    setLatest(null);
    if (keepLocal) writePlanDraft(sessionId, next);
    else clearPlanDraft(sessionId);
    setNotice(
      keepLocal
        ? "Локальный вариант сохранён в черновике. Нажмите «Сохранить», чтобы заменить серверный план."
        : "Загружен серверный план.",
    );
  }
  return (
    <div className={ui.page}>
      <h1 className={ui.title}>
        {showPlan ? "Итоговый план 5 × 5" : "Результаты по направлениям"}
      </h1>
      <div className={ui.actions}>
        <button
          className={ui.button}
          aria-pressed={!showPlan}
          onClick={() => setParams({})}
        >
          Результаты
        </button>
        <button
          className={ui.button}
          aria-pressed={showPlan}
          onClick={() => setParams({ view: "plan" })}
        >
          План · {draft.composition.universities.length}/5 вузов
        </button>
      </div>
      <div className={ui.actions}>
        <Link to="/onboarding/interests">Направления</Link>
        <Link to="/onboarding/olympiads">Олимпиады</Link>
        <Link to="/onboarding/achievements">ИД</Link>
        <Link to="/onboarding/privileges">Льготы</Link>
      </div>
      {dirty && (
        <p className={ui.notice}>Есть несохранённые изменения плана.</p>
      )}
      {notice && (
        <p className={ui.notice} role="status">
          {notice}
        </p>
      )}
      {error !== undefined && <ApiError error={error} />}
      {conflict && (
        <section className={ui.card} role="alert">
          <h2>План изменён в другой вкладке</h2>
          <p>
            Ваши правки сохранены в черновике. Сравните варианты перед
            сохранением.
          </p>
          {!latest ? (
            <button
              className={ui.button}
              disabled={busy}
              onClick={refreshConflict}
            >
              Загрузить серверный вариант
            </button>
          ) : (
            <>
              <p>Серверный план, версия {latest.version}:</p>
              {!latest.composition.universities.length && <p>Пустой список</p>}
              {latest.composition.universities.map((u, index) => (
                <div key={u.universityId}>
                  <strong>
                    {index + 1}.{" "}
                    {latest.options.find(
                      (p) => p.universityId === u.universityId,
                    )?.universityName ?? u.universityId}
                  </strong>
                  <ol>
                    {u.programIds.map((id) => (
                      <li key={id}>
                        {latest.options.find((p) => p.programId === id)
                          ?.programName ?? id}
                        {latest.composition.bviProgramId === id ? " · БВИ" : ""}
                      </li>
                    ))}
                  </ol>
                </div>
              ))}
              <button
                className={ui.button}
                onClick={() => resolveConflict(false)}
              >
                Принять серверный план
              </button>
              <button
                className={ui.button}
                onClick={() => resolveConflict(true)}
              >
                Оставить мой черновик для замены
              </button>
            </>
          )}
        </section>
      )}
      {!showPlan ? (
        <>
          <p className={ui.muted}>
            Результаты учитывают данные профиля, олимпиады и ИД. Выберите
            программы для итогового плана. Все баллы рассчитаны сервером.
          </p>
          {directionsReady && !directions.length && (
            <p className={ui.notice}>
              Вы ещё не выбрали направления.{" "}
              <Link to="/onboarding/interests">Перейти к выбору</Link>
            </p>
          )}
          {directions.map((direction) => (
            <RecommendationGroup
              key={direction.id}
              direction={direction}
              sessionId={sessionId}
              draft={draft}
              disabled={busy}
              onAdd={add}
            />
          ))}
          <button
            className={ui.button}
            onClick={() => setParams({ view: "plan" })}
          >
            К итоговой расстановке
          </button>
        </>
      ) : (
        <>
          <p className={ui.muted}>
            До 5 вузов и до 5 различных направлений в каждом. Порядок сверху
            вниз задаёт приоритет. Для замены удалите позицию и добавьте другую
            из результатов.
          </p>
          {!draft.composition.universities.length && (
            <p className={ui.notice}>
              План пуст. Добавьте программы на вкладке «Результаты».
            </p>
          )}
          {saved.warnings.map((warning, i) => (
            <p className={ui.notice} key={i}>
              {warning}
            </p>
          ))}
          <fieldset
            disabled={busy}
            style={{ border: 0, padding: 0, margin: 0, minWidth: 0 }}
            className={ui.stack}
          >
            {draft.composition.universities.map((u, index) => (
              <section className={ui.card} key={u.universityId}>
                <h2>
                  {index + 1}.{" "}
                  {draft.options.find((p) => p.universityId === u.universityId)
                    ?.universityName ?? u.universityId}
                </h2>
                <p>{u.programIds.length}/5 направлений</p>
                <div className={ui.actions}>
                  <button
                    className={ui.button}
                    aria-label="Поднять вуз"
                    disabled={!index}
                    onClick={() =>
                      change({
                        ...draft.composition,
                        universities: moveItem(
                          draft.composition.universities,
                          index,
                          -1,
                        ),
                      })
                    }
                  >
                    ↑
                  </button>
                  <button
                    className={ui.button}
                    aria-label="Опустить вуз"
                    disabled={
                      index === draft.composition.universities.length - 1
                    }
                    onClick={() =>
                      change({
                        ...draft.composition,
                        universities: moveItem(
                          draft.composition.universities,
                          index,
                          1,
                        ),
                      })
                    }
                  >
                    ↓
                  </button>
                  <button
                    className={ui.button}
                    onClick={() =>
                      change({
                        universities: draft.composition.universities.filter(
                          (item) => item.universityId !== u.universityId,
                        ),
                        bviProgramId: u.programIds.includes(
                          draft.composition.bviProgramId ?? "",
                        )
                          ? null
                          : draft.composition.bviProgramId,
                      })
                    }
                  >
                    Удалить вуз
                  </button>
                </div>
                {u.programIds.map((id, rank) => {
                  const option = draft.options.find((p) => p.programId === id);
                  const controls = (
                    <div className={ui.stack}>
                      <span>Приоритет направления: {rank + 1}</span>
                      <div className={ui.actions}>
                        {[-1, 1].map((delta) => (
                          <button
                            key={delta}
                            className={ui.button}
                            aria-label={
                              delta < 0
                                ? "Поднять направление"
                                : "Опустить направление"
                            }
                            disabled={
                              rank + delta < 0 ||
                              rank + delta >= u.programIds.length
                            }
                            onClick={() =>
                              change({
                                ...draft.composition,
                                universities:
                                  draft.composition.universities.map((item) =>
                                    item.universityId === u.universityId
                                      ? {
                                          ...item,
                                          programIds: moveItem(
                                            item.programIds,
                                            rank,
                                            delta,
                                          ),
                                        }
                                      : item,
                                  ),
                              })
                            }
                          >
                            {delta < 0 ? "↑" : "↓"}
                          </button>
                        ))}
                        <button
                          className={ui.button}
                          onClick={() =>
                            change(removeProgram(draft.composition, id))
                          }
                        >
                          Удалить программу
                        </button>
                      </div>
                      {(option?.bviAvailable ||
                        draft.composition.bviProgramId === id) && (
                        <label className={ui.check}>
                          <input
                            type="checkbox"
                            checked={draft.composition.bviProgramId === id}
                            onChange={(e) =>
                              change({
                                ...draft.composition,
                                bviProgramId: e.target.checked ? id : null,
                              })
                            }
                          />
                          Использовать БВИ здесь
                          {!option?.bviAvailable
                            ? " — право утрачено, снимите выбор"
                            : " (только одно место)"}
                        </label>
                      )}
                    </div>
                  );
                  return option ? (
                    <ProgramCard key={id} option={option}>
                      {controls}
                    </ProgramCard>
                  ) : (
                    <div key={id}>
                      {id}
                      {controls}
                    </div>
                  );
                })}
              </section>
            ))}
            <div className={ui.actions}>
              <button className={ui.button} onClick={() => setParams({})}>
                Добавить из результатов
              </button>
              <button
                className={ui.button}
                disabled={!draft.composition.universities.length}
                onClick={() => {
                  if (
                    window.confirm(
                      "Очистить черновик плана? Изменение сохранится на сервере только после нажатия «Сохранить».",
                    )
                  )
                    change(emptyComposition());
                }}
              >
                Очистить план
              </button>
            </div>
          </fieldset>
          <button
            className={ui.button}
            disabled={busy || conflict || !dirty}
            onClick={save}
          >
            {busy ? "Сохранение…" : "Сохранить план"}
          </button>
          {saved.savedAt && (
            <p className={ui.muted}>
              Последнее сохранение:{" "}
              {new Date(saved.savedAt).toLocaleString("ru-RU")}
            </p>
          )}
        </>
      )}
    </div>
  );
}

export function PlanningPage() {
  const sessionId = useAppSelector((s) => s.session.sessionId)!;
  const [params, setParams] = useSearchParams();
  const showPlan = params.get("view") === "plan";
  const loadSelection = useCallback(() => getSelectedDirections(sessionId), [sessionId]);
  const loadPlan = useCallback(() => getApplicationPlan(sessionId), [sessionId]);
  const selection = useRemote(loadSelection);
  const plan = useRemote(loadPlan);
  // IDs are enough to request recommendations. Catalogue labels must never block them.
  const directions = (selection.data?.directionIds ?? []).map((id) => ({ id, code: id, name: "" }));
  const unavailableDraft: PlanDraft = { composition: emptyComposition(), options: [], expectedVersion: 0 };
  return (
    <AppLayout>
      {selection.loading && <p className={ui.page} role="status">Загрузка выбранных направлений…</p>}
      {selection.error !== undefined && <div className={ui.page}><ApiError error={selection.error} retry={selection.retry} /></div>}
      {plan.data ? (
        <PlanWorkspace key={sessionId} sessionId={sessionId} initial={plan.data} directions={directions} directionsReady={!!selection.data} />
      ) : (
        <div className={ui.page}>
          <h1 className={ui.title}>{showPlan ? "Итоговый план 5 × 5" : "Результаты по направлениям"}</h1>
          <div className={ui.actions}>
            <button className={ui.button} aria-pressed={!showPlan} onClick={() => setParams({})}>Результаты</button>
            <button className={ui.button} aria-pressed={showPlan} onClick={() => setParams({ view: "plan" })}>План 5×5</button>
          </div>
          <section className={ui.notice}>
            <p>{plan.loading ? "План загружается. Рекомендации можно просматривать независимо." : "План временно недоступен. Рекомендации доступны, добавление в план — после его загрузки."}</p>
            {plan.error !== undefined && <ApiError error={plan.error} retry={plan.retry} />}
          </section>
          {!showPlan && <>
            {selection.data && !directions.length && <p>Вы ещё не выбрали направления.</p>}
            {directions.map((direction) => <RecommendationGroup key={direction.id} direction={direction} sessionId={sessionId} draft={unavailableDraft} disabled onAdd={() => {}} />)}
          </>}
          <Link to="/onboarding/interests">Вернуться к направлениям</Link>
        </div>
      )}
    </AppLayout>
  );
}
