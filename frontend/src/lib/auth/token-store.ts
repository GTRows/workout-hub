/**
 * Minimal browser-side token store. Access token lives in memory only (for
 * refresh-interceptor use); refresh token persists in localStorage so a
 * page reload can re-acquire an access token. LocalStorage is acceptable
 * for a self-hosted homelab app; rotate to HttpOnly cookies later if the
 * threat model tightens.
 */

const REFRESH_KEY = "wh.refreshToken";
const SESSION_COOKIE = "wh.hasSession";

function setSessionCookie(present: boolean): void {
  if (typeof document === "undefined") return;
  if (present) {
    // Presence-only marker used by Next middleware to gate /(app) routes.
    // The real access token lives in memory, the refresh token in
    // localStorage; this cookie carries zero auth weight on its own.
    document.cookie = `${SESSION_COOKIE}=1; Path=/; SameSite=Lax`;
  } else {
    document.cookie = `${SESSION_COOKIE}=; Path=/; Max-Age=0; SameSite=Lax`;
  }
}

let inMemoryAccessToken: string | null = null;
const subscribers = new Set<(token: string | null) => void>();

export function getAccessToken(): string | null {
  return inMemoryAccessToken;
}

export function setAccessToken(token: string | null): void {
  inMemoryAccessToken = token;
  subscribers.forEach((fn) => fn(token));
}

export function subscribeAccessToken(fn: (token: string | null) => void): () => void {
  subscribers.add(fn);
  return () => subscribers.delete(fn);
}

export function getRefreshToken(): string | null {
  if (typeof window === "undefined") return null;
  return window.localStorage.getItem(REFRESH_KEY);
}

export function setRefreshToken(token: string | null): void {
  if (typeof window === "undefined") return;
  if (token) {
    window.localStorage.setItem(REFRESH_KEY, token);
    setSessionCookie(true);
  } else {
    window.localStorage.removeItem(REFRESH_KEY);
    setSessionCookie(false);
  }
}

export function clearTokens(): void {
  setAccessToken(null);
  setRefreshToken(null);
}
