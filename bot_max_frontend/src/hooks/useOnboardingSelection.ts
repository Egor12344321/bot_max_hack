import { useState } from "react";
import { useAppSelector } from "@/store/hooks";
import { readOnboardingSelection, writeOnboardingSelection, type SelectionField } from "@/utils/onboardingDraft";

export function useOnboardingSelection(field: SelectionField) {
  const sessionId = useAppSelector((state) => state.session.sessionId);
  const [selectedIds, setSelectedIds] = useState<string[]>(() =>
    sessionId ? readOnboardingSelection(sessionId, field) : []);

  function updateSelection(ids: string[]) {
    setSelectedIds(ids);
    if (sessionId) writeOnboardingSelection(sessionId, field, ids);
  }

  return [selectedIds, updateSelection] as const;
}
