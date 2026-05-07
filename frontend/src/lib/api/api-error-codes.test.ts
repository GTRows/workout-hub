import { describe, expect, it } from "vitest";
import { ApiError } from "@/lib/api/client";
import {
  ApiErrorCode,
  KNOWN_API_ERROR_CODES,
  apiErrorCodeMessageKey,
  isApiError,
  isApiErrorWithCode,
  isKnownApiErrorCode,
} from "@/lib/api/api-error-codes";

describe("api-error-codes", () => {
  it("KNOWN_API_ERROR_CODES contains the 4 v0.4 codes", () => {
    expect(KNOWN_API_ERROR_CODES).toHaveLength(4);
    expect(KNOWN_API_ERROR_CODES).toContain("SESSION_ALREADY_ACTIVE");
    expect(KNOWN_API_ERROR_CODES).toContain("SESSION_ALREADY_FINISHED");
    expect(KNOWN_API_ERROR_CODES).toContain("SESSION_FINISHED");
    expect(KNOWN_API_ERROR_CODES).toContain("SET_NUMBER_DUPLICATE");
  });

  it("ApiErrorCode constants mirror KNOWN_API_ERROR_CODES values", () => {
    for (const code of KNOWN_API_ERROR_CODES) {
      expect(ApiErrorCode[code]).toBe(code);
    }
  });

  it("isKnownApiErrorCode returns true for known codes", () => {
    for (const code of KNOWN_API_ERROR_CODES) {
      expect(isKnownApiErrorCode(code)).toBe(true);
    }
  });

  it("isKnownApiErrorCode returns false for unknown strings and undefined", () => {
    expect(isKnownApiErrorCode("FUTURE_CODE")).toBe(false);
    expect(isKnownApiErrorCode("")).toBe(false);
    expect(isKnownApiErrorCode(undefined)).toBe(false);
  });

  it("isApiError narrows unknown to ApiError instances", () => {
    const err = new ApiError(409, {}, "msg", "SESSION_FINISHED");
    expect(isApiError(err)).toBe(true);
    expect(isApiError(new Error("plain"))).toBe(false);
    expect(isApiError(null)).toBe(false);
    expect(isApiError({ status: 409 })).toBe(false);
  });

  it("isApiErrorWithCode narrows on instance AND code match", () => {
    const finished = new ApiError(409, {}, "finished", "SESSION_FINISHED");
    expect(isApiErrorWithCode(finished, ApiErrorCode.SESSION_FINISHED)).toBe(true);

    const dup = new ApiError(409, {}, "dup", "SET_NUMBER_DUPLICATE");
    expect(isApiErrorWithCode(dup, ApiErrorCode.SESSION_FINISHED)).toBe(false);

    expect(isApiErrorWithCode(new Error("nope"), ApiErrorCode.SESSION_FINISHED)).toBe(false);
  });

  it.each([
    ["SESSION_ALREADY_ACTIVE", "errors.api.sessionAlreadyActive"],
    ["SESSION_ALREADY_FINISHED", "errors.api.sessionAlreadyFinished"],
    ["SESSION_FINISHED", "errors.api.sessionFinished"],
    ["SET_NUMBER_DUPLICATE", "errors.api.setNumberDuplicate"],
  ])("apiErrorCodeMessageKey maps %s to %s", (code, key) => {
    expect(apiErrorCodeMessageKey(code)).toBe(key);
  });

  it("apiErrorCodeMessageKey returns errors.api.generic for unknown codes and undefined", () => {
    expect(apiErrorCodeMessageKey("FUTURE_CODE")).toBe("errors.api.generic");
    expect(apiErrorCodeMessageKey(undefined)).toBe("errors.api.generic");
    expect(apiErrorCodeMessageKey("")).toBe("errors.api.generic");
  });
});
