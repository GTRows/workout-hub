import { describe, it, expect } from "vitest";
import { addSetRequestSchema } from "./schemas";

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
