import { AppError } from "@/utils/appError";

const API_URL = import.meta.env.VITE_API_URL;

let accessToken: string | null = null;

export function setAccessToken(token: string) {
  accessToken = token;
}

export function clearAccessToken() {
  accessToken = null;
}

export async function request<T>(
  path: string,
  options: RequestInit = {},
): Promise<T> {
  const headers = new Headers(options.headers);

  headers.set("Content-Type", "application/json");

  if (accessToken) {
    headers.set("Authorization", `Bearer ${accessToken}`);
  }

  let response: Response;
  try {
    response = await fetch(`${API_URL}${path}`, {
      ...options,
      headers,
    });
  } catch {
    throw new AppError("NETWORK_ERROR");
  }

  if (!response.ok) {
    let message = "Ошибка запроса";
    let code = "HTTP_ERROR";

    try {
      const error = await response.json();

      if (typeof error?.code === "string" && error.code.trim()) {
        code = error.code;
      }
      if (typeof error?.message === "string") {
        message = error.message;
      }
    } catch {
      // ничего не делаем
    }

    throw new AppError(code, response.status, message);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  const text = await response.text();
  return text.trim() ? JSON.parse(text) as T : undefined as T;
}
