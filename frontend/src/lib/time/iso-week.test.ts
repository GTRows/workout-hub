import { describe, expect, it } from "vitest";
import { getCurrentIsoWeekRange, isInRange } from "@/lib/time/iso-week";

describe("getCurrentIsoWeekRange", () => {
  it("returns Mon 00:00 -> Sun 23:59:59.999 from a Wednesday", () => {
    const wed = new Date(2026, 4, 6, 14, 30, 0);
    const { start, end } = getCurrentIsoWeekRange(wed);
    expect(start.getFullYear()).toBe(2026);
    expect(start.getMonth()).toBe(4);
    expect(start.getDate()).toBe(4);
    expect(start.getHours()).toBe(0);
    expect(start.getMinutes()).toBe(0);
    expect(start.getSeconds()).toBe(0);
    expect(start.getMilliseconds()).toBe(0);
    expect(end.getFullYear()).toBe(2026);
    expect(end.getMonth()).toBe(4);
    expect(end.getDate()).toBe(10);
    expect(end.getHours()).toBe(23);
    expect(end.getMinutes()).toBe(59);
    expect(end.getSeconds()).toBe(59);
    expect(end.getMilliseconds()).toBe(999);
  });

  it("treats Sunday as the last day of the same week", () => {
    const sun = new Date(2026, 4, 10, 22, 0, 0);
    const { start, end } = getCurrentIsoWeekRange(sun);
    expect(start.getDate()).toBe(4);
    expect(start.getMonth()).toBe(4);
    expect(end.getDate()).toBe(10);
    expect(end.getMonth()).toBe(4);
  });

  it("treats Monday at 00:00 as the start of that week", () => {
    const mon = new Date(2026, 4, 4, 0, 0, 0);
    const { start, end } = getCurrentIsoWeekRange(mon);
    expect(start.getTime()).toBe(mon.getTime());
    expect(end.getDate()).toBe(10);
    expect(end.getMonth()).toBe(4);
  });

  it("crosses month boundary correctly", () => {
    const wed = new Date(2026, 3, 29, 12, 0, 0);
    const { start, end } = getCurrentIsoWeekRange(wed);
    expect(start.getMonth()).toBe(3);
    expect(start.getDate()).toBe(27);
    expect(end.getMonth()).toBe(4);
    expect(end.getDate()).toBe(3);
  });

  it("isInRange returns true for a date inside the week and false for outside", () => {
    const wed = new Date(2026, 4, 6, 12, 0, 0);
    const range = getCurrentIsoWeekRange(wed);
    expect(isInRange(new Date(2026, 4, 5, 10, 0, 0), range)).toBe(true);
    expect(isInRange(new Date(2026, 4, 11, 10, 0, 0), range)).toBe(false);
  });

  it("isInRange accepts both Date and string", () => {
    const wed = new Date(2026, 4, 6, 12, 0, 0);
    const range = getCurrentIsoWeekRange(wed);
    const insideDate = new Date(2026, 4, 5, 10, 0, 0);
    const insideIso = insideDate.toISOString();
    expect(isInRange(insideDate, range)).toBe(true);
    expect(isInRange(insideIso, range)).toBe(true);
  });
});
