export interface ErrorDetails {
  status: number | null;
  code: string;
}

export class AppError extends Error {
  readonly status: number | null;
  readonly code: string;

  constructor(code: string, status: number | null = null, message = code) {
    super(message);
    this.name = "AppError";
    this.status = status;
    this.code = code;
  }
}

export function getErrorDetails(error: unknown): ErrorDetails {
  if (error instanceof AppError) {
    return { status: error.status, code: error.code };
  }
  return { status: null, code: "UNKNOWN_ERROR" };
}
