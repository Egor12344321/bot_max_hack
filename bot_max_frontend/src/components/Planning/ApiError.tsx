import { getErrorDetails } from "@/utils/appError";
import styles from "./Planning.module.css";

export function ApiError({
  error,
  retry,
}: {
  error: unknown;
  retry?: () => void;
}) {
  const { status } = getErrorDetails(error);
  const message =
    status === 401
      ? "Сессия истекла. Откройте приложение заново из бота."
      : status === 404
        ? "Данные не найдены или этот раздел пока недоступен на сервере."
        : error instanceof Error
          ? error.message
          : "Не удалось загрузить данные. Проверьте соединение.";
  return (
    <div className={styles.error} role="alert">
      {message}
      {status && <span> (HTTP {status})</span>}
      {retry && (
        <div>
          <button className={styles.button} onClick={retry}>
            Повторить
          </button>
        </div>
      )}
    </div>
  );
}
