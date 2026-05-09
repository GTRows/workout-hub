import { beforeEach, describe, expect, it } from "vitest";
import { POST } from "./route";
import { getRegistry } from "@/lib/metrics/registry";

function makeRequest(body: unknown): Request {
  return new Request("http://localhost/api/vitals", {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: typeof body === "string" ? body : JSON.stringify(body),
  });
}

describe("/api/vitals POST", () => {
  beforeEach(() => {
    getRegistry().reset();
  });

  it("records a valid LCP payload (ms -> seconds) and returns 204", async () => {
    const res = await POST(
      makeRequest({
        name: "LCP",
        value: 2500,
        id: "v3-1",
        navigationType: "navigate",
        route: "/dashboard",
      }),
    );
    expect(res.status).toBe(204);
    const body = await getRegistry().renderProm();
    expect(body).toContain(
      'wh_frontend_web_vital_seconds_bucket{name="LCP",route="/dashboard",le="2.5"} 1',
    );
  });

  it("records a valid CLS payload (unitless) and returns 204", async () => {
    const res = await POST(
      makeRequest({
        name: "CLS",
        value: 0.07,
        id: "v3-2",
        route: "/dashboard",
      }),
    );
    expect(res.status).toBe(204);
    const body = await getRegistry().renderProm();
    expect(body).toContain(
      'wh_frontend_web_vital_score_bucket{name="CLS",route="/dashboard",le="0.1"} 1',
    );
  });

  it("returns 400 for an invalid name and does not touch the registry", async () => {
    const res = await POST(
      makeRequest({
        name: "BOGUS",
        value: 1,
        id: "x",
        route: "/dashboard",
      }),
    );
    expect(res.status).toBe(400);
    const body = await getRegistry().renderProm();
    expect(body).not.toContain("wh_frontend_web_vital_seconds_bucket{name=");
    expect(body).not.toContain("wh_frontend_web_vital_score_bucket{name=");
  });

  it("returns 400 for malformed JSON", async () => {
    const req = new Request("http://localhost/api/vitals", {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: "not-json{",
    });
    const res = await POST(req);
    expect(res.status).toBe(400);
  });

  it("returns 400 for a negative value", async () => {
    const res = await POST(
      makeRequest({
        name: "LCP",
        value: -1,
        id: "x",
        route: "/dashboard",
      }),
    );
    expect(res.status).toBe(400);
  });

  it("normalizes id-bearing routes before recording", async () => {
    const res = await POST(
      makeRequest({
        name: "LCP",
        value: 1500,
        id: "v3-3",
        route: "/exercises/abc123",
      }),
    );
    expect(res.status).toBe(204);
    const body = await getRegistry().renderProm();
    expect(body).toContain(
      'wh_frontend_web_vital_seconds_count{name="LCP",route="/exercises/[id]"} 1',
    );
    expect(body).not.toContain('route="/exercises/abc123"');
  });
});
