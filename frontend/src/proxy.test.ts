import { beforeEach, describe, expect, it } from "vitest";
import { NextRequest } from "next/server";
import { PROTECTED_PREFIXES, config, proxy } from "./proxy";
import { getRegistry } from "@/lib/metrics/registry";

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

const EXPECTED_OPERATOR_MATCHERS = [
  "/api/healthz",
  "/api/livez",
  "/api/metrics",
];

describe("proxy route coverage", () => {
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
  });

  it("config.matcher includes the operator paths for metric instrumentation", () => {
    for (const op of EXPECTED_OPERATOR_MATCHERS) {
      expect(config.matcher).toContain(op);
    }
  });

  it("config.matcher enumerates the protected routes followed by operator paths", () => {
    const expectedProtected = PROTECTED_PREFIXES.map((p) => `${p}/:path*`);
    expect(config.matcher).toEqual([...expectedProtected, ...EXPECTED_OPERATOR_MATCHERS]);
  });
});

describe("proxy redirect behavior", () => {
  beforeEach(() => {
    getRegistry().reset();
  });

  it("redirects unauthenticated /achievements requests to /login with next param", () => {
    const req = new NextRequest(new URL("http://localhost/achievements"));
    const res = proxy(req);

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
    const res = proxy(req);

    expect(res.headers.get("location")).toBeNull();
    expect(res.status).toBe(200);
  });

  it("redirects unauthenticated /dashboard requests to /login (regression for existing route)", () => {
    const req = new NextRequest(new URL("http://localhost/dashboard"));
    const res = proxy(req);

    expect([307, 308]).toContain(res.status);
    const location = res.headers.get("location");
    expect(location).not.toBeNull();
    const target = new URL(location as string, "http://localhost");
    expect(target.pathname).toBe("/login");
    expect(target.searchParams.get("next")).toBe("/dashboard");
  });
});

describe("proxy metric recording", () => {
  beforeEach(() => {
    getRegistry().reset();
  });

  it("records a 2xx counter entry for /api/healthz pass-through", () => {
    const req = new NextRequest(new URL("http://localhost/api/healthz"));
    proxy(req);
    const out = getRegistry().renderProm();
    expect(out).toContain(
      'wh_frontend_http_requests_total{method="GET",route="/api/healthz",status_class="2xx"} 1',
    );
  });

  it("records a 3xx counter entry when an unauthenticated /dashboard request is redirected", () => {
    const req = new NextRequest(new URL("http://localhost/dashboard"));
    proxy(req);
    const out = getRegistry().renderProm();
    expect(out).toContain(
      'wh_frontend_http_requests_total{method="GET",route="/dashboard",status_class="3xx"} 1',
    );
  });

  it("does not record any counter for /api/metrics scrapes", () => {
    const req = new NextRequest(new URL("http://localhost/api/metrics"));
    proxy(req);
    const out = getRegistry().renderProm();
    expect(out).not.toContain('route="/api/metrics"');
  });

  it("collapses /session/<id> requests into the [id] label", () => {
    const req = new NextRequest(new URL("http://localhost/session/abc-123"), {
      headers: { cookie: "wh.hasSession=1" },
    });
    proxy(req);
    const out = getRegistry().renderProm();
    expect(out).toContain('route="/session/[id]"');
    expect(out).not.toContain('route="/session/abc-123"');
  });
});
