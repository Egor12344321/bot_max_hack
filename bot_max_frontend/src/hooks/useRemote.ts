import { useEffect, useState } from "react";

export function useRemote<T>(loader: () => Promise<T>) {
  const [revision, setRevision] = useState(0);
  const [result, setResult] = useState<{
    loader: typeof loader;
    revision: number;
    data?: T;
    error?: unknown;
  }>();
  useEffect(() => {
    let active = true;
    loader()
      .then((data) => {
        if (active) setResult({ loader, revision, data });
      })
      .catch((error: unknown) => {
        if (active) setResult({ loader, revision, error });
      });
    return () => {
      active = false;
    };
  }, [loader, revision]);
  const current =
    result?.loader === loader && result.revision === revision
      ? result
      : undefined;
  return {
    data: current?.data,
    error: current?.error,
    loading: !current,
    retry: () => setRevision((n) => n + 1),
  };
}
