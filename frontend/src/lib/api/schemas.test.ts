import { describe, it, expect } from "vitest";
import {
  addSetRequestSchema,
  fullExportSchema,
  importResultSchema,
  pushSubscribeRequestSchema,
  vapidPublicKeyResponseSchema,
} from "./schemas";

const validUuid = "11111111-1111-4111-8111-111111111111";
const validClientSetId = "22222222-2222-4222-8222-222222222222";

describe("addSetRequestSchema", () => {
  it("accepts a minimal valid payload (exerciseId + repsDone)", () => {
    const result = addSetRequestSchema.parse({
      exerciseId: validUuid,
      repsDone: 10,
    });
    expect(result.exerciseId).toBe(validUuid);
    expect(result.repsDone).toBe(10);
    expect(result.clientSetId).toBeUndefined();
  });

  it("accepts a full valid payload with clientSetId, rpe, notes, weightKg, completed, setNumber", () => {
    const payload = {
      exerciseId: validUuid,
      setNumber: 3,
      repsDone: 8,
      weightKg: 80,
      rpe: 8,
      completed: true,
      notes: "felt strong",
      clientSetId: validClientSetId,
    };
    const result = addSetRequestSchema.parse(payload);
    expect(result).toEqual(payload);
  });

  it("rejects an invalid uuid for exerciseId", () => {
    expect(() =>
      addSetRequestSchema.parse({ exerciseId: "not-a-uuid", repsDone: 5 })
    ).toThrow();
  });

  it("rejects an invalid uuid for clientSetId", () => {
    expect(() =>
      addSetRequestSchema.parse({
        exerciseId: validUuid,
        repsDone: 5,
        clientSetId: "abc",
      })
    ).toThrow();
  });

  it("rejects negative repsDone", () => {
    expect(() =>
      addSetRequestSchema.parse({ exerciseId: validUuid, repsDone: -1 })
    ).toThrow();
  });

  it("rejects rpe out of 1..10 range", () => {
    expect(() =>
      addSetRequestSchema.parse({ exerciseId: validUuid, repsDone: 5, rpe: 0 })
    ).toThrow();
    expect(() =>
      addSetRequestSchema.parse({ exerciseId: validUuid, repsDone: 5, rpe: 11 })
    ).toThrow();
  });

  it("accepts an omitted clientSetId for backward compat with pre-Phase-25 payloads", () => {
    const result = addSetRequestSchema.parse({
      exerciseId: validUuid,
      repsDone: 10,
      completed: true,
    });
    expect(result.clientSetId).toBeUndefined();
  });
});

describe("importResultSchema", () => {
  it("accepts a minimal valid payload (counts + email; no warnings/suggestions)", () => {
    const result = importResultSchema.parse({
      profileUpdated: 1,
      metricsInserted: 0,
      supplementsInserted: 0,
      plansInserted: 0,
      sessionsInserted: 0,
      userEmail: "user@example.com",
    });
    expect(result.userEmail).toBe("user@example.com");
    expect(result.warnings).toBeUndefined();
    expect(result.suggestions).toBeUndefined();
  });

  it("accepts a full valid payload with non-empty warnings and suggestions", () => {
    const result = importResultSchema.parse({
      profileUpdated: 1,
      metricsInserted: 12,
      supplementsInserted: 3,
      plansInserted: 1,
      sessionsInserted: 24,
      userEmail: "import@example.com",
      warnings: ["session 5 had no exercises"],
      suggestions: ["consider running an export first"],
    });
    expect(result.warnings).toEqual(["session 5 had no exercises"]);
    expect(result.suggestions).toEqual(["consider running an export first"]);
  });

  it("rejects negative metricsInserted", () => {
    expect(() =>
      importResultSchema.parse({
        profileUpdated: 0,
        metricsInserted: -1,
        supplementsInserted: 0,
        plansInserted: 0,
        sessionsInserted: 0,
        userEmail: "u@e.com",
      })
    ).toThrow();
  });
});

describe("fullExportSchema", () => {
  it("accepts a minimal valid envelope (schemaVersion 1, empty arrays, populated user)", () => {
    const result = fullExportSchema.parse({
      schemaVersion: 1,
      exportedAt: "2026-05-09T00:00:00Z",
      user: {
        id: validUuid,
        email: "u@e.com",
        displayName: "Tester",
        heightCm: null,
        weightKg: null,
        birthDate: null,
        gender: null,
        healthNotes: null,
        goals: null,
      },
      plans: [],
      sessions: [],
      bodyMetrics: [],
      supplements: [],
    });
    expect(result.schemaVersion).toBe(1);
    expect(result.user.id).toBe(validUuid);
  });

  it("rejects payloads missing schemaVersion", () => {
    expect(() =>
      fullExportSchema.parse({
        exportedAt: "2026-05-09T00:00:00Z",
        user: {
          id: validUuid,
          email: "u@e.com",
          displayName: "Tester",
          heightCm: null,
          weightKg: null,
          birthDate: null,
          gender: null,
          healthNotes: null,
          goals: null,
        },
        plans: [],
        sessions: [],
        bodyMetrics: [],
        supplements: [],
      })
    ).toThrow();
  });

  it("accepts a session with a 9-field SetRow including isPr true", () => {
    const result = fullExportSchema.parse({
      schemaVersion: 1,
      exportedAt: "2026-05-09T00:00:00Z",
      user: {
        id: validUuid,
        email: "u@e.com",
        displayName: "Tester",
        heightCm: null,
        weightKg: null,
        birthDate: null,
        gender: null,
        healthNotes: null,
        goals: null,
      },
      plans: [],
      sessions: [
        {
          id: validUuid,
          workoutDayId: null,
          startedAt: "2026-05-09T10:00:00Z",
          endedAt: "2026-05-09T11:00:00Z",
          notes: null,
          mood: null,
          energyLevel: null,
          sets: [
            {
              id: validClientSetId,
              exerciseId: validUuid,
              setNumber: 1,
              repsDone: 5,
              weightKg: 100,
              rpe: 8,
              completed: true,
              notes: null,
              isPr: true,
            },
          ],
        },
      ],
      bodyMetrics: [],
      supplements: [],
    });
    expect(result.sessions[0].sets[0].isPr).toBe(true);
  });
});

describe("vapidPublicKeyResponseSchema", () => {
  it("accepts a populated public key", () => {
    const result = vapidPublicKeyResponseSchema.parse({
      publicKey: "BNcRdreALRFXTkOPCtNF1tzJ5n1BzGm9TpQz5z2qP7K0WJzM4mZxQYZN0",
    });
    expect(result.publicKey.length).toBeGreaterThan(0);
  });

  it("accepts an empty publicKey (backend unconfigured VAPID case)", () => {
    const result = vapidPublicKeyResponseSchema.parse({ publicKey: "" });
    expect(result.publicKey).toBe("");
  });

  it("rejects payloads missing publicKey", () => {
    expect(() => vapidPublicKeyResponseSchema.parse({})).toThrow();
  });
});

describe("pushSubscribeRequestSchema", () => {
  it("accepts a minimal valid body (endpoint + keys)", () => {
    const result = pushSubscribeRequestSchema.parse({
      endpoint: "https://fcm.googleapis.com/fcm/send/abc",
      keys: { p256dh: "BNc...", auth: "Dxq..." },
    });
    expect(result.userAgent).toBeUndefined();
  });

  it("accepts a full valid body including userAgent", () => {
    const result = pushSubscribeRequestSchema.parse({
      endpoint: "https://fcm.googleapis.com/fcm/send/abc",
      keys: { p256dh: "BNc...", auth: "Dxq..." },
      userAgent: "Mozilla/5.0 (Windows)",
    });
    expect(result.userAgent).toBe("Mozilla/5.0 (Windows)");
  });

  it("rejects body missing keys.p256dh", () => {
    expect(() =>
      pushSubscribeRequestSchema.parse({
        endpoint: "https://fcm.googleapis.com/fcm/send/abc",
        keys: { auth: "Dxq..." },
      })
    ).toThrow();
  });

  it("rejects empty-string endpoint", () => {
    expect(() =>
      pushSubscribeRequestSchema.parse({
        endpoint: "",
        keys: { p256dh: "BNc...", auth: "Dxq..." },
      })
    ).toThrow();
  });
});
