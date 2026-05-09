import { beforeEach, describe, expect, it } from "vitest";
import { GET } from "./route";
import { getRegistry } from "@/lib/metrics/registry";

describe("/api/metrics", () => {
  beforeEach(() => {
    getRegistry().reset();
  });

  it("returns 200 with Prometheus text format", async () => {
    const res = GET();
    expect(res.status).toBe(200);
    expect(res.headers.get("content-type")).toContain("text/plain");
    const body = await res.text();
    expect(body).toContain("# HELP nodejs_process_uptime_seconds");
    expect(body).toContain("# TYPE nodejs_process_uptime_seconds gauge");
    expect(body).toMatch(/nodejs_process_uptime_seconds \d+(\.\d+)?/);
  });

  it("emits the HTTP request counter and histogram family declarations", async () => {
    const body = await GET().text();
    expect(body).toContain("# TYPE wh_frontend_http_requests_total counter");
    expect(body).toContain(
      "# TYPE wh_frontend_http_request_duration_seconds histogram",
    );
  });

  it("includes a counter line and histogram bucket after a recorded request", async () => {
    getRegistry().record("GET", "/api/healthz", 200, 5);
    const body = await GET().text();
    expect(body).toContain(
      'wh_frontend_http_requests_total{method="GET",route="/api/healthz",status_class="2xx"} 1',
    );
    expect(body).toContain(
      'wh_frontend_http_request_duration_seconds_bucket{method="GET",route="/api/healthz",le="0.01"} 1',
    );
    expect(body).toContain(
      'wh_frontend_http_request_duration_seconds_count{method="GET",route="/api/healthz"} 1',
    );
  });
});
