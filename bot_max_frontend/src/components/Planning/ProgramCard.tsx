import type { ReactNode } from "react";
import type { ProgramOption } from "@/api/types/planning";
import ui from "./Planning.module.css";

const eligibility = {
  eligible: "Условия допуска выполнены",
  incomplete: "Не хватает данных для проверки допуска",
  ineligible: "Условия допуска не выполнены",
};
const comparison = {
  bvi: "Доступно БВИ",
  above_previous: "Выше прошлогоднего проходного",
  near_previous: "Около прошлогоднего проходного",
  below_previous: "Ниже прошлогоднего проходного",
  insufficient_data: "Недостаточно данных для сравнения",
  ineligible: "Нет допуска к конкурсу",
};
const forms = {
  full_time: "Очная",
  part_time: "Очно-заочная",
  correspondence: "Заочная",
};
const competitions = {
  general: "Общий конкурс",
  special_quota: "Особая квота",
  separate_quota: "Отдельная квота",
  target_quota: "Целевая квота",
};
export function ProgramCard({
  option: p,
  children,
  showUniversity = true,
}: {
  option: ProgramOption;
  children?: ReactNode;
  showUniversity?: boolean;
}) {
  return (
    <article className={ui.card}>
      <details className={ui.programDisclosure}>
        <summary className={ui.programSummary}>
          <span className={ui.programHeading}>
            {showUniversity && <strong>{p.universityName}</strong>}
            <span>{p.programName}</span>
            <span className={ui.muted}>{p.direction.code} · {p.direction.name}</span>
            <span className={ui.disclosureHint}>Условия и расчёт</span>
          </span>
          <span className={ui.chevron} aria-hidden="true">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" focusable="false">
              <path d="m6 9 6 6 6-6" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
            </svg>
          </span>
        </summary>
        <div className={ui.programDescription}>
      <span className={ui.muted}>
        {[
          p.city,
          p.campus,
          `${p.campaignYear}`,
          p.funding === "budget" ? "Бюджет" : "Платное",
          forms[p.studyForm],
          competitions[p.competitionType],
        ]
          .filter(Boolean)
          .join(" · ")}
      </span>
      <p>{eligibility[p.eligibility]}</p>
      <div className={ui.stack}>
        <span>
          Ваш конкурсный балл: <strong>{p.totalScore ?? "Нет данных"}</strong>
        </span>
        <span>
          Проходной
          {p.previousYear !== null
            ? ` за ${p.previousYear} год`
            : " прошлого года"}
          : <strong>{p.passingScorePreviousYear ?? "Нет данных"}</strong>
        </span>
        <span>
          Разница:{" "}
          <strong>
            {p.scoreDifference === null
              ? "Нет сопоставимых данных"
              : `${p.scoreDifference > 0 ? "+" : ""}${p.scoreDifference}`}
          </strong>
        </span>
        <span className={ui.muted}>
          {comparison[p.comparison]}. Прошлогодний результат не гарантирует
          поступление.
        </span>
      </div>
      {p.reasons.length > 0 && (
        <ul>
          {p.reasons.map((reason, i) => (
            <li key={i}>{reason}</li>
          ))}
        </ul>
      )}
      <details>
        <summary>Как получен результат</summary>
        <div className={ui.details}>
          {!p.breakdown.length && <p>Детализация не предоставлена.</p>}
          {p.breakdown.map((item, i) => (
            <div key={i}>
              <strong>{item.name}</strong>: {item.originalScore ?? "—"} →{" "}
              {item.countedScore ?? "—"}
              <p className={ui.muted}>
                {item.applied ? "Учтено" : "Не учтено"}. {item.explanation}
              </p>
            </div>
          ))}
        </div>
      </details>
        </div>
      </details>
      {(p.dataSource === "demo" || p.bviAvailable) && (
        <div className={ui.actions}>
          {p.dataSource === "demo" && <span className={ui.badge}>Демонстрационные данные</span>}
          {p.bviAvailable && <span className={ui.badge}>БВИ доступно</span>}
        </div>
      )}
      {children}
    </article>
  );
}
