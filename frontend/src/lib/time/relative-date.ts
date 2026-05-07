/**
 * Relative-date classifier. Returns a discriminated union the consumer
 * translates via next-intl. The function is locale-agnostic; the
 * `kind: "absolute"` branch carries an ISO date string the consumer
 * formats with `Intl.DateTimeFormat`.
 */

export type RelativeDate =
  | { kind: "today" }
  | { kind: "daysAgo"; days: number }
  | { kind: "absolute"; iso: string };

const MS_PER_DAY = 86_400_000;

function startOfDay(d: Date): Date {
  const out = new Date(d);
  out.setHours(0, 0, 0, 0);
  return out;
}

function toIsoDate(d: Date): string {
  const yyyy = d.getFullYear();
  const mm = String(d.getMonth() + 1).padStart(2, "0");
  const dd = String(d.getDate()).padStart(2, "0");
  return `${yyyy}-${mm}-${dd}`;
}

export function formatRelativeDays(
  then: Date | string,
  now: Date = new Date()
): RelativeDate {
  const thenDate = typeof then === "string" ? new Date(then) : then;
  const t0 = startOfDay(thenDate).getTime();
  const n0 = startOfDay(now).getTime();
  const days = Math.round((n0 - t0) / MS_PER_DAY);
  if (days <= 0) return { kind: "today" };
  if (days <= 7) return { kind: "daysAgo", days };
  return { kind: "absolute", iso: toIsoDate(thenDate) };
}
