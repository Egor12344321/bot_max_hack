import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { initializeSession } from "@/app/initializeSession";
import { clearSession, initializationFailed, setInitialized, setSession } from "@/store/slices/sessionSlice";
import { useAppDispatch } from "@/store/hooks";

export function useAppInit() {
  const dispatch = useAppDispatch();
  const { i18n } = useTranslation();
  const [attempt, setAttempt] = useState(0);

  useEffect(() => {
    let cancelled = false;
    dispatch(clearSession());
    async function initApp() {
      try {
        const { session, accessToken } = await initializeSession();
        if (cancelled) return;
        await i18n.changeLanguage(session.language);
        if (cancelled) return;
        dispatch(setSession({
          sessionId: session.id,
          accessToken,
          language: session.language,
          isCompleteFromBot: session.isCompleteFromBot,
          egeScores: session.egeScores,
        }));
        dispatch(setInitialized(true));
      } catch {
        if (!cancelled) dispatch(initializationFailed());
      }
    }
    void initApp();
    return () => { cancelled = true; };
  }, [attempt, dispatch, i18n]);

  return { retry: () => setAttempt((current) => current + 1) };
}
