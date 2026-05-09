import { beforeEach, describe, expect, it } from "vitest";
import { getRegistry, HISTOGRAM_BUCKETS_SECONDS } from "./registry";

describe("metrics registry", () => {
  beforeEach(() => {
    getRegistry().reset();
  });

  it("records a single request and emits the matching counter line", () => {
    getRegistry().record("GET", "/api/healthz", 200, 12);
    const out = getRegistry().renderProm();
    expect(out).toContain(
      'wh_frontend_http_requests_total{method="GET",route="/api/healthz",status_class="2xx"} 1',
    );
  });

  it("classifies status codes into 1xx-5xx buckets", () => {
    const r = getRegistry();
    r.record("GET", "/a", 100, 1);
    r.record("GET", "/b", 204, 1);
    r.record("GET", "/c", 307, 1);
    r.record("GET", "/d", 404, 1);
    r.record("GET", "/e", 503, 1);
    const out = r.renderProm();
    expect(out).toContain('status_class="1xx"');
    expect(out).toContain('status_class="2xx"');
    expect(out).toContain('status_class="3xx"');
    expect(out).toContain('status_class="4xx"');
    expect(out).toContain('status_class="5xx"');
  });

  it("aggregates histogram count and sum across two recordings", () => {
    const r = getRegistry();
    r.record("GET", "/api/healthz", 200, 12);
    r.record("GET", "/api/healthz", 200, 600);
    const out = r.renderProm();
    const labelPrefix = 'method="GET",route="/api/healthz"';
    expect(out).toContain(
      `wh_frontend_http_request_duration_seconds_count{${labelPrefix}} 2`,
    );
    const sumMatch = out.match(
      new RegExp(
        `wh_frontend_http_request_duration_seconds_sum\\{${labelPrefix.replace(/[/]/g, "\\/")}\\} (\\S+)`,
      ),
    );
    expect(sumMatch).not.toBeNull();
    const sum = Number(sumMatch?.[1]);
    expect(sum).toBeCloseTo(0.612, 5);
  });

  it("places 12ms in the 0.025 bucket and 600ms in the 1 bucket", () => {
    const r = getRegistry();
    r.record("GET", "/api/healthz", 200, 12);
    r.record("GET", "/api/healthz", 200, 600);
    const out = r.renderProm();
    const labelPrefix = 'method="GET",route="/api/healthz"';
    expect(out).toContain(
      `wh_frontend_http_request_duration_seconds_bucket{${labelPrefix},le="0.005"} 0`,
    );
    expect(out).toContain(
      `wh_frontend_http_request_duration_seconds_bucket{${labelPrefix},le="0.01"} 0`,
    );
    expect(out).toContain(
      `wh_frontend_http_request_duration_seconds_bucket{${labelPrefix},le="0.025"} 1`,
    );
    expect(out).toContain(
      `wh_frontend_http_request_duration_seconds_bucket{${labelPrefix},le="0.5"} 1`,
    );
    expect(out).toContain(
      `wh_frontend_http_request_duration_seconds_bucket{${labelPrefix},le="1"} 2`,
    );
    expect(out).toContain(
      `wh_frontend_http_request_duration_seconds_bucket{${labelPrefix},le="+Inf"} 2`,
    );
  });

  it("emits HELP and TYPE family declarations even with zero samples", () => {
    const out = getRegistry().renderProm();
    expect(out).toMatch(/^# HELP wh_frontend_http_requests_total /m);
    expect(out).toMatch(/^# TYPE wh_frontend_http_requests_total counter$/m);
    expect(out).toMatch(/^# HELP wh_frontend_http_request_duration_seconds /m);
    expect(out).toMatch(/^# TYPE wh_frontend_http_request_duration_seconds histogram$/m);
  });

  it("reset() clears counters and histograms but keeps the singleton", () => {
    const before = getRegistry();
    before.record("GET", "/api/healthz", 200, 12);
    expect(before.renderProm()).toContain(
      'wh_frontend_http_requests_total{method="GET",route="/api/healthz",status_class="2xx"} 1',
    );
    before.reset();
    const after = getRegistry();
    expect(after).toBe(before);
    const out = after.renderProm();
    expect(out).not.toContain(
      'wh_frontend_http_requests_total{method="GET",route="/api/healthz",status_class="2xx"} 1',
    );
  });

  it("exports the standard Prometheus default bucket bounds", () => {
    expect(HISTOGRAM_BUCKETS_SECONDS).toEqual([
      0.005, 0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 1, 2.5, 5, 10,
    ]);
  });
});
