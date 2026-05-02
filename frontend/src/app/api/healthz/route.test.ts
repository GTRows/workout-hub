import { describe, it, expect } from "vitest";
import { GET } from "./route";

describe("/api/healthz", () => {
  it("returns 200 with status alive", async () => {
    const res = GET();
    expect(res.status).toBe(200);
    const body = await res.json();
    expect(body.status).toBe("alive");
  });
});
