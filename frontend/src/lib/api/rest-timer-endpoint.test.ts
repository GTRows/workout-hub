import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { cancelRestTimer, scheduleRestTimer } from "@/lib/api/endpoints";
import { clearTokens } from "@/lib/auth/token-store";

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "content-type": "application/json" },
  });
}

function noContent(): Response {
  return new Response(null, { status: 204 });
}

const SESSION_ID = "ffffffff-1111-1111-1111-111111111111";

describe("scheduleRestTimer", () => {
  let lastUrl = "";
  let lastInit: RequestInit | undefined;

  beforeEach(() => {
    clearTokens();
    lastUrl = "";
    lastInit = undefined;
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("POSTs to /api/sessions/{id}/rest-timer with the validated body", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async (input: string | URL | Request, init?: RequestInit) => {
        lastUrl = typeof input === "string" ? input : input.toString();
        lastInit = init;
        return jsonResponse(201, {
          id: "11111111-1111-1111-1111-111111111111",
          sessionId: SESSION_ID,
          fireAt: "2026-05-09T10:01:00Z",
        });
      })
    );

    const result = await scheduleRestTimer(SESSION_ID, {
      seconds: 60,
      title: "Rest over",
      body: "60s rest complete",
    });

    expect(lastUrl).toContain(`/api/sessions/${SESSION_ID}/rest-timer`);
    expect(lastInit?.method).toBe("POST");
    const body = JSON.parse(lastInit!.body as string) as {
      seconds: number;
      title: string;
      body: string;
    };
    expect(body.seconds).toBe(60);
    expect(body.title).toBe("Rest over");
    expect(body.body).toBe("60s rest complete");
    expect(result.fireAt).toBe("2026-05-09T10:01:00Z");
  });

  it("rejects an out-of-range seconds value via the Zod request schema", async () => {
    await expect(() =>
      scheduleRestTimer(SESSION_ID, {
        seconds: 0,
        title: "x",
        body: "y",
      })
    ).rejects.toThrow();

    await expect(() =>
      scheduleRestTimer(SESSION_ID, {
        seconds: 3601,
        title: "x",
        body: "y",
      })
    ).rejects.toThrow();
  });
});

describe("cancelRestTimer", () => {
  let lastUrl = "";
  let lastInit: RequestInit | undefined;

  beforeEach(() => {
    clearTokens();
    lastUrl = "";
    lastInit = undefined;
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("DELETEs to /api/sessions/{id}/rest-timer", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async (input: string | URL | Request, init?: RequestInit) => {
        lastUrl = typeof input === "string" ? input : input.toString();
        lastInit = init;
        return noContent();
      })
    );

    await cancelRestTimer(SESSION_ID);

    expect(lastUrl).toContain(`/api/sessions/${SESSION_ID}/rest-timer`);
    expect(lastInit?.method).toBe("DELETE");
  });
});
