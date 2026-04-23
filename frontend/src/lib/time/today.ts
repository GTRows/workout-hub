/**
 * ISO-8601 day of week: Monday = 1 ... Sunday = 7. Matches the V4 CHECK
 * constraint on workout_days.day_of_week, so this value feeds directly
 * into plan lookups.
 */
export function getTodayIsoDayOfWeek(now: Date = new Date()): number {
  const jsDay = now.getDay();
  return jsDay === 0 ? 7 : jsDay;
}
