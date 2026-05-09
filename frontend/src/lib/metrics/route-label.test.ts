import { describe, expect, it } from "vitest";
import { normalizeRoute } from "./route-label";

describe("normalizeRoute", () => {
  it("passes through exact operator endpoints", () => {
    expect(normalizeRoute("/api/healthz")).toBe("/api/healthz");
    expect(normalizeRoute("/api/livez")).toBe("/api/livez");
    expect(normalizeRoute("/api/metrics")).toBe("/api/metrics");
  });

  it("collapses /session/<id> to /session/[id]", () => {
    expect(normalizeRoute("/session/abc-123")).toBe("/session/[id]");
    expect(normalizeRoute("/session/00000000-0000-0000-0000-000000000000")).toBe(
      "/session/[id]",
    );
  });

  it("collapses /exercises/<id> to /exercises/[id]", () => {
    expect(normalizeRoute("/exercises/42")).toBe("/exercises/[id]");
    expect(normalizeRoute("/exercises/squat")).toBe("/exercises/[id]");
  });

  it("collapses /plan/<weekId> to /plan/[weekId]", () => {
    expect(normalizeRoute("/plan/2026-W19")).toBe("/plan/[weekId]");
    expect(normalizeRoute("/plan/abc")).toBe("/plan/[weekId]");
  });

  it("preserves the known /plan/editor sub-route", () => {
    expect(normalizeRoute("/plan/editor")).toBe("/plan/editor");
  });

  it("returns the prefix as-is for index paths", () => {
    expect(normalizeRoute("/session")).toBe("/session");
    expect(normalizeRoute("/plan")).toBe("/plan");
  });

  it("collapses /history /insights /prs /achievements /nutrition with ids", () => {
    expect(normalizeRoute("/history/123")).toBe("/history/[id]");
    expect(normalizeRoute("/insights/foo")).toBe("/insights/[id]");
    expect(normalizeRoute("/prs/x")).toBe("/prs/[id]");
    expect(normalizeRoute("/achievements/x")).toBe("/achievements/[id]");
    expect(normalizeRoute("/nutrition/x")).toBe("/nutrition/[id]");
  });

  it("passes through 1-2 segment unmatched paths verbatim", () => {
    expect(normalizeRoute("/")).toBe("/");
    expect(normalizeRoute("/login")).toBe("/login");
    expect(normalizeRoute("/dashboard")).toBe("/dashboard");
    expect(normalizeRoute("/api/auth")).toBe("/api/auth");
  });

  it("collapses unknown 3+ segment deep paths to /<a>/<b>/_other", () => {
    expect(normalizeRoute("/foo/bar/baz")).toBe("/foo/bar/_other");
    expect(normalizeRoute("/api/auth/login/extra")).toBe("/api/auth/_other");
  });

  it("never returns a raw user-controlled id segment under a known prefix", () => {
    const labels = [
      "/session/aaa",
      "/exercises/123",
      "/plan/2026-W19",
      "/history/0",
      "/insights/x",
      "/prs/y",
      "/achievements/z",
      "/nutrition/q",
    ].map(normalizeRoute);
    for (const label of labels) {
      expect(label).toMatch(/\[(id|weekId)\]$/);
    }
  });
});
