/**
 * Normalises a request pathname into a low-cardinality Prometheus label.
 *
 * Cardinality control is the single most important property here: any path
 * containing a user-controlled identifier collapses into a `[id]`-style
 * placeholder. Unknown deep paths collapse to `_other` to bound the label
 * set even if a future route is added without an entry below.
 */

const EXACT_PASSTHROUGH = new Set<string>([
  "/api/healthz",
  "/api/livez",
  "/api/metrics",
]);

const ID_SEGMENTS: ReadonlyArray<{ prefix: string; placeholder: string }> = [
  { prefix: "/session", placeholder: "/session/[id]" },
  { prefix: "/exercises", placeholder: "/exercises/[id]" },
  { prefix: "/plan", placeholder: "/plan/[weekId]" },
  { prefix: "/history", placeholder: "/history/[id]" },
  { prefix: "/insights", placeholder: "/insights/[id]" },
  { prefix: "/prs", placeholder: "/prs/[id]" },
  { prefix: "/achievements", placeholder: "/achievements/[id]" },
  { prefix: "/nutrition", placeholder: "/nutrition/[id]" },
];

const PLAN_KNOWN_SUBROUTES = new Set<string>(["editor"]);

export function normalizeRoute(pathname: string): string {
  if (EXACT_PASSTHROUGH.has(pathname)) {
    return pathname;
  }

  for (const rule of ID_SEGMENTS) {
    if (pathname === rule.prefix) {
      return rule.prefix;
    }
    const childPrefix = `${rule.prefix}/`;
    if (pathname.startsWith(childPrefix)) {
      const rest = pathname.slice(childPrefix.length);
      if (rest.length === 0) {
        return rule.prefix;
      }
      const firstSegment = rest.split("/")[0];
      if (rule.prefix === "/plan" && PLAN_KNOWN_SUBROUTES.has(firstSegment)) {
        return `/plan/${firstSegment}`;
      }
      return rule.placeholder;
    }
  }

  const segments = pathname.split("/").filter((s) => s.length > 0);
  if (segments.length >= 3) {
    return `/${segments[0]}/${segments[1]}/_other`;
  }
  return pathname;
}
