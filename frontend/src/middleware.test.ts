import { describe, expect, it } from "vitest";
import { NextRequest } from "next/server";
import { PROTECTED_PREFIXES, config, middleware } from "./middleware";

const EXPECTED_PROTECTED_ROUTES = [
  "dashboard",
  "plan",
  "history",
  "exercises",
  "insights",
  "prs",
  "achievements",
  "nutrition",
  "metrics",
  "profile",
  "export",
  "session",
];

describe("middleware route coverage", () => {
  it("PROTECTED_PREFIXES covers every (app)-group route segment", () => {
    for (const seg of EXPECTED_PROTECTED_ROUTES) {
      expect(PROTECTED_PREFIXES).toContain(`/${seg}`);
    }
    expect(PROTECTED_PREFIXES).toHaveLength(EXPECTED_PROTECTED_ROUTES.length);
  });

  it("config.matcher covers every (app)-group route segment with :path* suffix", () => {
    for (const seg of EXPECTED_PROTECTED_ROUTES) {
      expect(config.matcher).toContain(`/${seg}/:path*`);
    }
    expect(config.matcher).toHaveLength(EXPECTED_PROTECTED_ROUTES.length);
  });

  it("PROTECTED_PREFIXES and config.matcher enumerate the same routes", () => {
    const expectedMatcher = PROTECTED_PREFIXES.map((p) => `${p}/:path*`);
    expect(config.matcher).toEqual(expectedMatcher);
  });
});

describe("middleware redirect behavior", () => {
  it("redirects unauthenticated /achievements requests to /login with next param", () => {
    const req = new NextRequest(new URL("http://localhost/achievements"));
    const res = middleware(req);

    expect([307, 308]).toContain(res.status);
    const location = res.headers.get("location");
    expect(location).not.toBeNull();
    const target = new URL(location as string, "http://localhost");
    expect(target.pathname).toBe("/login");
    expect(target.searchParams.get("next")).toBe("/achievements");
  });

  it("passes through authenticated /achievements requests", () => {
    const req = new NextRequest(new URL("http://localhost/achievements"), {
      headers: { cookie: "wh.hasSession=1" },
    });
    const res = middleware(req);

    expect(res.headers.get("location")).toBeNull();
    expect(res.status).toBe(200);
  });

  it("redirects unauthenticated /dashboard requests to /login (regression for existing route)", () => {
    const req = new NextRequest(new URL("http://localhost/dashboard"));
    const res = middleware(req);

    expect([307, 308]).toContain(res.status);
    const location = res.headers.get("location");
    expect(location).not.toBeNull();
    const target = new URL(location as string, "http://localhost");
    expect(target.pathname).toBe("/login");
    expect(target.searchParams.get("next")).toBe("/dashboard");
  });
});
