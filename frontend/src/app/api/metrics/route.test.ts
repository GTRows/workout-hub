import { describe, it, expect } from "vitest";
import { GET } from "./route";

describe("/api/metrics", () => {
  it("returns 200 with Prometheus text format", async () => {
    const res = GET();
    expect(res.status).toBe(200);
    expect(res.headers.get("content-type")).toContain("text/plain");
    const body = await res.text();
    expect(body).toContain("# HELP nodejs_process_uptime_seconds");
    expect(body).toContain("# TYPE nodejs_process_uptime_seconds gauge");
    expect(body).toMatch(/nodejs_process_uptime_seconds \d+(\.\d+)?/);
  });
});
