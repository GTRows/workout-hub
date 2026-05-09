import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { fetchExerciseProgress } from "@/lib/api/endpoints";
import { progressPointSchema } from "@/lib/api/schemas";
import { clearTokens } from "@/lib/auth/token-store";

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "content-type": "application/json" },
  });
}

const EXERCISE_ID = "ffffffff-1111-1111-1111-111111111111";

describe("fetchExerciseProgress", () => {
  let lastUrl = "";

  beforeEach(() => {
    clearTokens();
    lastUrl = "";
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  function stubFetch(payload: unknown) {
    vi.stubGlobal(
      "fetch",
      vi.fn(async (input: string | URL | Request) => {
        lastUrl = typeof input === "string" ? input : input.toString();
        return jsonResponse(200, payload);
      })
    );
  }

  it("calls /api/exercises/{id}/progress with the default limit when limit is omitted", async () => {
    stubFetch([]);
    await fetchExerciseProgress(EXERCISE_ID);
    expect(lastUrl).toContain(`/api/exercises/${EXERCISE_ID}/progress`);
    expect(lastUrl).toContain("limit=10");
  });

  it("clamps out-of-range limit values to the default 10", async () => {
    stubFetch([]);

    await fetchExerciseProgress(EXERCISE_ID, 200);
    expect(lastUrl).toContain("limit=10");

    await fetchExerciseProgress(EXERCISE_ID, 0);
    expect(lastUrl).toContain("limit=10");

    await fetchExerciseProgress(EXERCISE_ID, 5);
    expect(lastUrl).toContain("limit=5");
  });

  it("Zod-parses a representative ProgressPoint payload", async () => {
    const payload = [
      {
        sessionId: "11111111-1111-1111-1111-111111111111",
        startedAt: "2026-05-01T12:00:00Z",
        setCount: 3,
        totalVolumeKg: 1500.5,
        maxWeightKg: 80,
        topRepsDone: 8,
        estimatedOneRmKg: 92.5,
      },
    ];
    stubFetch(payload);

    const result = await fetchExerciseProgress(EXERCISE_ID);
    expect(result).toHaveLength(1);
    expect(result[0]?.estimatedOneRmKg).toBe(92.5);

    // Direct schema parse cross-check.
    expect(() => progressPointSchema.parse(payload[0])).not.toThrow();
  });
});
