/**
 * ISO-week range helpers. ISO weeks start on Monday and end on Sunday;
 * boundaries are local-time. Both helpers are pure and `now`-injectable
 * so unit tests can pin the clock.
 */

export type IsoWeekRange = { start: Date; end: Date };

export function getCurrentIsoWeekRange(now: Date = new Date()): IsoWeekRange {
  const offset = (now.getDay() + 6) % 7;
  const start = new Date(now);
  start.setDate(now.getDate() - offset);
  start.setHours(0, 0, 0, 0);
  const end = new Date(start);
  end.setDate(start.getDate() + 6);
  end.setHours(23, 59, 59, 999);
  return { start, end };
}

export function isInRange(
  when: Date | string,
  range: IsoWeekRange
): boolean {
  const t = typeof when === "string" ? new Date(when).getTime() : when.getTime();
  return t >= range.start.getTime() && t <= range.end.getTime();
}
