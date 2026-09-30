import { useId, useState, type ReactNode } from "react";
import type { ProgramOption } from "@/api/types/planning";
import { ProgramCard } from "./ProgramCard";
import ui from "./Planning.module.css";

export function PlanProgramRow({ id, rank, option, bvi, children }: {
  id: string;
  rank: number;
  option?: ProgramOption;
  bvi: boolean;
  children: ReactNode;
}) {
  const [expanded, setExpanded] = useState(false);
  const detailId = useId();
  return (
    <>
      <tr>
        <td className={ui.rank}>{rank + 1}</td>
        <th scope="row">
          <button className={ui.programToggle} aria-expanded={expanded} aria-controls={detailId} onClick={() => setExpanded(!expanded)}>
            <span>{option?.programName ?? id}</span>
            <span className={ui.muted}>{option?.direction.code ?? "Нет данных"}{bvi ? " · БВИ выбрано" : option?.bviAvailable ? " · БВИ доступно" : ""} · {expanded ? "Свернуть" : "Подробнее"}</span>
            {option?.dataSource === "demo" && <span className={ui.muted}>Демо</span>}
          </button>
        </th>
        <td className={ui.planScore}>{option?.totalScore ?? "—"}<span>/ {option?.passingScorePreviousYear ?? "—"}</span></td>
      </tr>
      <tr id={detailId} hidden={!expanded}>
        <td colSpan={3} className={ui.expandedCell}>
          {expanded && <div className={ui.stack}>{children}{option && <ProgramCard option={option} showUniversity={false} />}</div>}
        </td>
      </tr>
    </>
  );
}
