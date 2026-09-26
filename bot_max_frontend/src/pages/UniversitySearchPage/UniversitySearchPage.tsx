import { useEffect, useState } from "react";
import { Button, Typography } from "@maxhub/max-ui";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import { addUniversity, getUniversities, searchUniversities } from "@/api/universitiesApi";
import type { UniversityCard } from "@/api/types/universities";
import { AppLayout } from "@/components/AppLayout/AppLayout";
import { useAppSelector } from "@/store/hooks";
import styles from "./UniversitySearchPage.module.css";

export function UniversitySearchPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const sessionId = useAppSelector((state) => state.session.sessionId);
  const [query, setQuery] = useState("");
  const [results, setResults] = useState<UniversityCard[]>([]);
  const [selectedIds, setSelectedIds] = useState<string[]>([]);
  const [loadedQuery, setLoadedQuery] = useState<string | null>(null);
  const [loadedSessionId, setLoadedSessionId] = useState<string | null>(null);
  const loading = loadedQuery !== query;
  const ready = loadedSessionId === sessionId && sessionId !== null;
  const [addingId, setAddingId] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!sessionId) return;
    let cancelled = false;
    getUniversities(sessionId).then((items) => {
      if (!cancelled) {
        setSelectedIds(items.map((item) => item.id));
        setLoadedSessionId(sessionId);
      }
    }).catch(() => {
      if (!cancelled) setError(t("universities.loadError"));
    });
    return () => { cancelled = true; };
  }, [sessionId, t]);

  useEffect(() => {
    let cancelled = false;
    const timer = setTimeout(() => {
      searchUniversities(query).then((items) => {
        if (!cancelled) setResults(items);
      }).catch(() => {
        if (!cancelled) {
          setResults([]);
          setError(t("universities.loadError"));
        }
      }).finally(() => {
        if (!cancelled) setLoadedQuery(query);
      });
    }, 250);
    return () => { cancelled = true; clearTimeout(timer); };
  }, [query, t]);

  async function handleAdd(id: string) {
    if (!sessionId || !ready || addingId || selectedIds.includes(id)) return;
    setAddingId(id);
    setError(null);
    try {
      await addUniversity(sessionId, id);
      setSelectedIds((current) => [...current, id]);
    } catch {
      setError(t("universities.addError"));
    } finally {
      setAddingId(null);
    }
  }

  return (
    <AppLayout>
      <div className={styles.page}>
        <Button variant="secondary" onClick={() => navigate("/universities")}>
          {t("common.back")}
        </Button>
        <Typography.Title>{t("universities.addUniversity")}</Typography.Title>
        <input
          className={styles.search}
          type="search"
          aria-label={t("universities.searchPlaceholder")}
          placeholder={t("universities.searchPlaceholder")}
          value={query}
          onChange={(event) => setQuery(event.target.value)}
        />
        {error && <div className={styles.error} role="alert">{error}</div>}
        {loading ? <Typography.Body>{t("common.loading")}</Typography.Body> : (
          <div className={styles.list}>
            {results.length === 0 && <Typography.Body>{t("universities.noResults")}</Typography.Body>}
            {results.map((item) => {
              const added = selectedIds.includes(item.id);
              return (
                <article key={item.id} className={styles.card}>
                  <div>
                    <strong>{item.name}</strong>
                    <div className={styles.city}>{item.city}</div>
                  </div>
                  <Button
                    variant="secondary"
                    disabled={!ready || added || addingId !== null}
                    loading={addingId === item.id}
                    onClick={() => handleAdd(item.id)}
                  >
                    {t(added ? "universities.added" : "universities.add")}
                  </Button>
                </article>
              );
            })}
          </div>
        )}
      </div>
    </AppLayout>
  );
}
