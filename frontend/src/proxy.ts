import { NextResponse, type NextRequest } from "next/server";
import { getRegistry } from "@/lib/metrics/registry";
import { normalizeRoute } from "@/lib/metrics/route-label";

/**
 * Protected-route guard (Next 16 proxy; renamed from middleware in v1.1
 * Phase 48 to close i-6b). Any path under the (app) group (dashboard, plan,
 * history, exercises, metrics, profile, export, session) is gated: a
 * missing refresh-token cookie sends the caller to /login with a ?next=
 * redirect preserved.
 *
 * We check a cookie (set by the client after login) rather than reading
 * localStorage because the proxy runs on the edge and has no DOM access.
 * The client mirrors the refresh token into a cookie on login; the
 * in-memory access token continues to drive actual API calls.
 *
 * Per Phase 35, the proxy is also the recording point for the
 * frontend HTTP metrics registry. Every matched request is timed and
 * counted; `/api/metrics` is excluded so the scrape itself does not
 * pollute the histogram.
 */
export const PROTECTED_PREFIXES = [
  "/dashboard",
  "/plan",
  "/history",
  "/exercises",
  "/insights",
  "/prs",
  "/achievements",
  "/nutrition",
  "/metrics",
  "/profile",
  "/export",
  "/session",
];

function guardOrPassThrough(req: NextRequest): NextResponse {
  const { pathname } = req.nextUrl;
  const isProtected = PROTECTED_PREFIXES.some((p) => pathname.startsWith(p));
  if (!isProtected) return NextResponse.next();

  const hasSession = req.cookies.get("wh.hasSession")?.value === "1";
  if (hasSession) return NextResponse.next();

  const loginUrl = new URL("/login", req.url);
  loginUrl.searchParams.set("next", pathname);
  return NextResponse.redirect(loginUrl);
}

function inferStatus(res: NextResponse): number {
  const status = res.status;
  if (status && status !== 0) return status;
  return 200;
}

export function proxy(req: NextRequest): NextResponse {
  const start = Date.now();
  const res = guardOrPassThrough(req);
  const duration = Date.now() - start;
  const { pathname } = req.nextUrl;
  if (pathname !== "/api/metrics") {
    getRegistry().record(
      req.method,
      normalizeRoute(pathname),
      inferStatus(res),
      duration,
    );
  }
  return res;
}

export const config = {
  matcher: [
    "/dashboard/:path*",
    "/plan/:path*",
    "/history/:path*",
    "/exercises/:path*",
    "/insights/:path*",
    "/prs/:path*",
    "/achievements/:path*",
    "/nutrition/:path*",
    "/metrics/:path*",
    "/profile/:path*",
    "/export/:path*",
    "/session/:path*",
    "/api/healthz",
    "/api/livez",
    "/api/metrics",
  ],
};
