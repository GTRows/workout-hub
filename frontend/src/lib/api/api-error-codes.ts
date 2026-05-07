import { ApiError } from "@/lib/api/client";

export const KNOWN_API_ERROR_CODES = [
  "SESSION_ALREADY_ACTIVE",
  "SESSION_ALREADY_FINISHED",
  "SESSION_FINISHED",
  "SET_NUMBER_DUPLICATE",
] as const;

export type KnownApiErrorCode = (typeof KNOWN_API_ERROR_CODES)[number];

export const ApiErrorCode = {
  SESSION_ALREADY_ACTIVE: "SESSION_ALREADY_ACTIVE",
  SESSION_ALREADY_FINISHED: "SESSION_ALREADY_FINISHED",
  SESSION_FINISHED: "SESSION_FINISHED",
  SET_NUMBER_DUPLICATE: "SET_NUMBER_DUPLICATE",
} as const satisfies Record<KnownApiErrorCode, KnownApiErrorCode>;

export function isKnownApiErrorCode(
  value: string | undefined
): value is KnownApiErrorCode {
  return (
    value !== undefined &&
    (KNOWN_API_ERROR_CODES as readonly string[]).includes(value)
  );
}

export function isApiError(err: unknown): err is ApiError {
  return err instanceof ApiError;
}

export function isApiErrorWithCode(err: unknown, code: string): err is ApiError {
  return isApiError(err) && err.code === code;
}

const CODE_TO_KEY: Readonly<Record<KnownApiErrorCode, string>> = {
  SESSION_ALREADY_ACTIVE: "errors.api.sessionAlreadyActive",
  SESSION_ALREADY_FINISHED: "errors.api.sessionAlreadyFinished",
  SESSION_FINISHED: "errors.api.sessionFinished",
  SET_NUMBER_DUPLICATE: "errors.api.setNumberDuplicate",
};

export function apiErrorCodeMessageKey(code: string | undefined): string {
  if (isKnownApiErrorCode(code)) {
    return CODE_TO_KEY[code];
  }
  return "errors.api.generic";
}
