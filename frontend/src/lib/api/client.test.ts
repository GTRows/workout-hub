import { beforeEach, describe, expect, it, vi } from "vitest";
import { createApiClient } from "@/lib/api/client";
import {
  clearTokens,
  getAccessToken,
  getRefreshToken,
  setAccessToken,
  setRefreshToken,
} from "@/lib/auth/token-store";

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "content-type": "application/json" },
  });
}

describe("api client refresh interceptor", () => {
  beforeEach(() => {
    clearTokens();
    window.localStorage.clear();
  });

  it("retries a 401 with a freshly-refreshed access token", async () => {
    setAccessToken("expired-access");
    setRefreshToken("valid-refresh");

    const fetchImpl = vi
      .fn()
      .mockImplementationOnce(async () => jsonResponse(401, { message: "expired" }))
      .mockImplementationOnce(async () =>
        jsonResponse(200, {
          accessToken: "fresh-access",
          refreshToken: "fresh-refresh",
          userId: "11111111-1111-1111-1111-111111111111",
          role: "USER",
        })
      )
      .mockImplementationOnce(async () =>
        jsonResponse(200, { id: "x", email: "x@y.z", displayName: "X", role: "USER", profile: {} })
      );

    const api = createApiClient({ baseUrl: "http://api", fetchImpl });
    const result = await api.request({ path: "/api/users/me" });

    expect(result).toBeDefined();
    expect(fetchImpl).toHaveBeenCalledTimes(3);
    expect(fetchImpl.mock.calls[1][0]).toBe("http://api/api/auth/refresh");
    expect(getAccessToken()).toBe("fresh-access");
    expect(getRefreshToken()).toBe("fresh-refresh");
    const retriedHeaders = new Headers(fetchImpl.mock.calls[2][1].headers);
    expect(retriedHeaders.get("authorization")).toBe("Bearer fresh-access");
  });

  it("clears tokens when the refresh endpoint itself returns 401", async () => {
    setAccessToken("expired-access");
    setRefreshToken("revoked-refresh");

    const fetchImpl = vi
      .fn()
      .mockImplementationOnce(async () => jsonResponse(401, { message: "expired" }))
      .mockImplementationOnce(async () => jsonResponse(401, { message: "revoked" }));

    const api = createApiClient({ baseUrl: "http://api", fetchImpl });
    await expect(api.request({ path: "/api/users/me" })).rejects.toThrowError();

    expect(getAccessToken()).toBeNull();
    expect(getRefreshToken()).toBeNull();
  });

  it("does not attempt refresh when auth:false is passed", async () => {
    const fetchImpl = vi
      .fn()
      .mockResolvedValue(jsonResponse(401, { message: "nope" }));

    const api = createApiClient({ baseUrl: "http://api", fetchImpl });
    await expect(
      api.request({ path: "/api/auth/login", method: "POST", body: {}, auth: false })
    ).rejects.toThrowError();

    expect(fetchImpl).toHaveBeenCalledTimes(1);
  });
});

describe("api client ApiError propagation", () => {
  beforeEach(() => {
    clearTokens();
    window.localStorage.clear();
  });

  it("populates ApiError.code when the backend response includes code", async () => {
    const fetchImpl = vi.fn().mockResolvedValue(
      jsonResponse(409, {
        timestamp: "2026-05-07T00:00:00Z",
        status: 409,
        error: "Conflict",
        message: "Session already finished",
        code: "SESSION_FINISHED",
        path: "/api/sessions/abc/sets",
      })
    );

    const api = createApiClient({ baseUrl: "http://api", fetchImpl });
    await expect(
      api.request({ path: "/api/sessions/abc/sets", method: "POST", body: {}, auth: false })
    ).rejects.toMatchObject({
      name: "ApiError",
      status: 409,
      code: "SESSION_FINISHED",
    });
  });

  it("leaves ApiError.code undefined when the backend response omits code", async () => {
    const fetchImpl = vi.fn().mockResolvedValue(
      jsonResponse(400, {
        timestamp: "2026-05-07T00:00:00Z",
        status: 400,
        error: "Bad Request",
        message: "Validation failed",
        path: "/api/auth/login",
      })
    );

    const api = createApiClient({ baseUrl: "http://api", fetchImpl });
    await expect(
      api.request({ path: "/api/auth/login", method: "POST", body: {}, auth: false })
    ).rejects.toMatchObject({
      name: "ApiError",
      status: 400,
      code: undefined,
    });
  });
});

describe("api client returnStatus overload", () => {
  beforeEach(() => {
    clearTokens();
    window.localStorage.clear();
  });

  it("propagates the HTTP status alongside the parsed body when returnStatus:true", async () => {
    const fetchImpl = vi
      .fn()
      .mockResolvedValue(jsonResponse(201, { id: "abc", value: 42 }));

    const api = createApiClient({ baseUrl: "http://api", fetchImpl });
    const result = await api.request({
      path: "/api/things",
      method: "POST",
      body: { value: 42 },
      auth: false,
      returnStatus: true,
    });

    expect(result).toEqual({
      data: { id: "abc", value: 42 },
      status: 201,
    });
  });

  it("returns { data: undefined, status: 204 } for a 204 response when returnStatus:true", async () => {
    const fetchImpl = vi
      .fn()
      .mockResolvedValue(new Response(null, { status: 204 }));

    const api = createApiClient({ baseUrl: "http://api", fetchImpl });
    const result = await api.request({
      path: "/api/things/abc",
      method: "DELETE",
      auth: false,
      returnStatus: true,
    });

    expect(result).toEqual({ data: undefined, status: 204 });
  });

  it("still throws ApiError on a 4xx response when returnStatus:true", async () => {
    const fetchImpl = vi.fn().mockResolvedValue(
      jsonResponse(409, {
        timestamp: "2026-05-07T00:00:00Z",
        status: 409,
        error: "Conflict",
        message: "Already exists",
        code: "DUPLICATE",
        path: "/api/things",
      })
    );

    const api = createApiClient({ baseUrl: "http://api", fetchImpl });
    await expect(
      api.request({
        path: "/api/things",
        method: "POST",
        body: {},
        auth: false,
        returnStatus: true,
      })
    ).rejects.toMatchObject({
      name: "ApiError",
      status: 409,
      code: "DUPLICATE",
    });
  });
});
