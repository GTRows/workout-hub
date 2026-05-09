import { z } from "zod";
import {
  clearTokens,
  getAccessToken,
  getRefreshToken,
  setAccessToken,
  setRefreshToken,
} from "@/lib/auth/token-store";
import { apiErrorSchema, authResponseSchema } from "@/lib/api/schemas";

const DEFAULT_BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

export class ApiError extends Error {
  constructor(
    public readonly status: number,
    public readonly body: unknown,
    message: string,
    public readonly code: string | undefined = undefined
  ) {
    super(message);
    this.name = "ApiError";
  }
}

type RequestOptions<R> = {
  method?: "GET" | "POST" | "PUT" | "PATCH" | "DELETE";
  path: string;
  body?: unknown;
  schema?: z.ZodType<R>;
  auth?: boolean;
  query?: Record<string, string | number | boolean | undefined>;
  signal?: AbortSignal;
  returnStatus?: boolean;
};

export type RequestWithStatus<R> = { data: R; status: number };

type ClientDeps = {
  baseUrl?: string;
  fetchImpl?: typeof fetch;
};

export function createApiClient(deps: ClientDeps = {}) {
  const baseUrl = deps.baseUrl ?? DEFAULT_BASE_URL;
  // Late-binding: resolve fetch on every call so tests can stub globalThis.fetch
  // after module load without having to rebuild the client.
  const fetchImpl: typeof fetch = (input, init) =>
    (deps.fetchImpl ?? (globalThis as { fetch: typeof fetch }).fetch)(input, init);

  let refreshPromise: Promise<string | null> | null = null;

  async function refreshAccessToken(): Promise<string | null> {
    if (refreshPromise) return refreshPromise;
    const refreshToken = getRefreshToken();
    if (!refreshToken) return null;

    refreshPromise = fetchImpl(`${baseUrl}/api/auth/refresh`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ refreshToken }),
    })
      .then(async (res) => {
        if (!res.ok) {
          clearTokens();
          return null;
        }
        const body = authResponseSchema.parse(await res.json());
        setAccessToken(body.accessToken);
        setRefreshToken(body.refreshToken);
        return body.accessToken;
      })
      .catch(() => {
        clearTokens();
        return null;
      })
      .finally(() => {
        refreshPromise = null;
      });

    return refreshPromise;
  }

  async function raw(input: string, init: RequestInit, retry = true): Promise<Response> {
    const headers = new Headers(init.headers);
    const token = getAccessToken();
    if (token && !headers.has("authorization")) {
      headers.set("authorization", `Bearer ${token}`);
    }
    const res = await fetchImpl(input, { ...init, headers });
    if (res.status === 401 && retry) {
      const newAccess = await refreshAccessToken();
      if (newAccess) {
        const retryHeaders = new Headers(init.headers);
        retryHeaders.set("authorization", `Bearer ${newAccess}`);
        return fetchImpl(input, { ...init, headers: retryHeaders });
      }
    }
    return res;
  }

  function request<R = unknown>(
    opts: RequestOptions<R> & { returnStatus: true }
  ): Promise<RequestWithStatus<R>>;
  function request<R = unknown>(opts: RequestOptions<R>): Promise<R>;
  async function request<R = unknown>(
    opts: RequestOptions<R>
  ): Promise<R | RequestWithStatus<R>> {
    const qs =
      opts.query && Object.keys(opts.query).length > 0
        ? "?" +
          Object.entries(opts.query)
            .filter(([, v]) => v !== undefined)
            .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(String(v))}`)
            .join("&")
        : "";

    const init: RequestInit = {
      method: opts.method ?? "GET",
      headers: { "content-type": "application/json", accept: "application/json" },
      signal: opts.signal,
    };
    if (opts.body !== undefined) init.body = JSON.stringify(opts.body);

    const res = await raw(`${baseUrl}${opts.path}${qs}`, init, opts.auth !== false);
    const status = res.status;

    if (status === 204) {
      const data = undefined as R;
      return opts.returnStatus === true ? { data, status } : data;
    }

    const text = await res.text();
    const json = text ? JSON.parse(text) : undefined;

    if (!res.ok) {
      const parsed = apiErrorSchema.safeParse(json);
      const message = parsed.success ? parsed.data.message : res.statusText;
      const code = parsed.success ? parsed.data.code : undefined;
      throw new ApiError(status, json, message, code);
    }

    const data: R = opts.schema ? opts.schema.parse(json) : (json as R);
    return opts.returnStatus === true ? { data, status } : data;
  }

  return { request, raw, refreshAccessToken };
}

export const api = createApiClient();
