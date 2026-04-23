import { NextResponse, type NextRequest } from "next/server";

/**
 * Protected-route guard. Any path under the (app) group (dashboard, plan,
 * history, exercises, metrics, profile, export, session) is gated: a
 * missing refresh-token cookie sends the caller to /login with a ?next=
 * redirect preserved.
 *
 * We check a cookie (set by the client after login) rather than reading
 * localStorage because middleware runs on the edge and has no DOM access.
 * The client mirrors the refresh token into a cookie on login; the
 * in-memory access token continues to drive actual API calls.
 */
const PROTECTED_PREFIXES = [
  "/dashboard",
  "/plan",
  "/history",
  "/exercises",
  "/insights",
  "/metrics",
  "/profile",
  "/export",
  "/session",
];

export function middleware(req: NextRequest) {
  const { pathname } = req.nextUrl;
  const isProtected = PROTECTED_PREFIXES.some((p) => pathname.startsWith(p));
  if (!isProtected) return NextResponse.next();

  const hasSession = req.cookies.get("wh.hasSession")?.value === "1";
  if (hasSession) return NextResponse.next();

  const loginUrl = new URL("/login", req.url);
  loginUrl.searchParams.set("next", pathname);
  return NextResponse.redirect(loginUrl);
}

export const config = {
  matcher: [
    "/dashboard/:path*",
    "/plan/:path*",
    "/history/:path*",
    "/exercises/:path*",
    "/insights/:path*",
    "/metrics/:path*",
    "/profile/:path*",
    "/export/:path*",
    "/session/:path*",
  ],
};
