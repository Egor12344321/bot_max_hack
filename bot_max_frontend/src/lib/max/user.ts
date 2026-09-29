/**
 * Имя пользователя из launch params MAX (поле user). Бэкенд имя не хранит.
 * Вне MAX и в мок-режиме возвращает null.
 */
export function getMaxUserName(): string | null {
  const initData = window.WebApp?.initData;
  if (!initData) return null;
  try {
    const raw = new URLSearchParams(initData).get("user");
    if (!raw) return null;
    const user = JSON.parse(raw) as { first_name?: string; last_name?: string };
    const name = [user.first_name, user.last_name].filter(Boolean).join(" ").trim();
    return name || null;
  } catch {
    return null;
  }
}
