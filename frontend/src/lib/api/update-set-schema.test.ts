import { describe, expect, it } from "vitest";
import { updateSetRequestSchema } from "./schemas";

describe("updateSetRequestSchema", () => {
  it("accepts an empty patch (all fields optional)", () => {
    expect(updateSetRequestSchema.parse({})).toEqual({});
  });

  it("accepts a full valid patch with repsDone, weightKg, rpe, completed, notes", () => {
    const patch = {
      repsDone: 8,
      weightKg: 80.5,
      rpe: 9,
      completed: true,
      notes: "felt strong",
    };
    expect(updateSetRequestSchema.parse(patch)).toEqual(patch);
  });

  it("rejects rpe out of 1..10 range", () => {
    expect(() => updateSetRequestSchema.parse({ rpe: 0 })).toThrow();
    expect(() => updateSetRequestSchema.parse({ rpe: 11 })).toThrow();
  });

  it("rejects negative repsDone", () => {
    expect(() => updateSetRequestSchema.parse({ repsDone: -1 })).toThrow();
  });

  it("rejects weightKg above 999.99", () => {
    expect(() => updateSetRequestSchema.parse({ weightKg: 1000 })).toThrow();
  });
});
