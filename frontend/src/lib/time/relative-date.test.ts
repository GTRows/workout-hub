import { describe, expect, it } from "vitest";
import { formatRelativeDays } from "@/lib/time/relative-date";

describe("formatRelativeDays", () => {
  it("returns today for a same-day timestamp", () => {
    const now = new Date(2026, 4, 6, 14, 0, 0);
    const earlier = new Date(2026, 4, 6, 2, 0, 0);
    const later = new Date(2026, 4, 6, 23, 30, 0);
    expect(formatRelativeDays(now, now)).toEqual({ kind: "today" });
    expect(formatRelativeDays(earlier, now)).toEqual({ kind: "today" });
    expect(formatRelativeDays(later, now)).toEqual({ kind: "today" });
  });

  it("returns daysAgo: 1 for yesterday", () => {
    const now = new Date(2026, 4, 6, 12, 0, 0);
    const yesterday = new Date(2026, 4, 5, 9, 0, 0);
    expect(formatRelativeDays(yesterday, now)).toEqual({
      kind: "daysAgo",
      days: 1,
    });
  });

  it("returns daysAgo: 7 for exactly 7 days ago (inclusive boundary)", () => {
    const now = new Date(2026, 4, 8, 12, 0, 0);
    const sevenDaysAgo = new Date(2026, 4, 1, 12, 0, 0);
    expect(formatRelativeDays(sevenDaysAgo, now)).toEqual({
      kind: "daysAgo",
      days: 7,
    });
  });

  it("returns absolute for 8 or more days ago", () => {
    const now = new Date(2026, 4, 9, 12, 0, 0);
    const eightDaysAgo = new Date(2026, 4, 1, 12, 0, 0);
    const result = formatRelativeDays(eightDaysAgo, now);
    expect(result.kind).toBe("absolute");
    if (result.kind === "absolute") {
      expect(result.iso).toBe("2026-05-01");
    }
  });

  it("treats future dates as today", () => {
    const now = new Date(2026, 4, 6, 12, 0, 0);
    const tomorrow = new Date(2026, 4, 7, 12, 0, 0);
    expect(formatRelativeDays(tomorrow, now)).toEqual({ kind: "today" });
  });

  it("accepts both Date and string for the then parameter", () => {
    const now = new Date(2026, 4, 6, 12, 0, 0);
    const yesterday = new Date(2026, 4, 5, 9, 0, 0);
    const yesterdayIso = yesterday.toISOString();
    expect(formatRelativeDays(yesterday, now)).toEqual(
      formatRelativeDays(yesterdayIso, now)
    );
  });
});
