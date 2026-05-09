# Frontend Performance Budgets

**Status:** Tracked, not gated. v0.6 ships budgets as written targets; v1.0
hardening (Phase 38+) will gate CI on them.

## Why

Budgets define when a regression is unacceptable. Without numbers, "the app
feels slow" is unactionable. The targets below are the baseline a future
release plan can compare against; the in-app collector emits live data so the
operator can verify whether real users meet them.

## Core Web Vitals targets

The targets follow web.dev's published Web Vitals thresholds
(https://web.dev/articles/vitals). Source URL is recorded so future readers
can re-verify.

| Metric | Good (target) | Needs improvement | Poor |
| --- | --- | --- | --- |
| LCP (Largest Contentful Paint) | <= 2.5s | 2.5s - 4.0s | > 4.0s |
| INP (Interaction to Next Paint) | <= 200ms | 200ms - 500ms | > 500ms |
| CLS (Cumulative Layout Shift) | <= 0.1 | 0.1 - 0.25 | > 0.25 |
| FCP (First Contentful Paint) | <= 1.8s | 1.8s - 3.0s | > 3.0s |
| TTFB (Time to First Byte) | <= 0.8s | 0.8s - 1.8s | > 1.8s |

The "good" column is the per-route p75 target the project aims to hit on
mobile-class hardware. Server-side TTFB (the back-end half) is also covered
by the Phase 35 `wh_frontend_http_request_duration_seconds` histogram; the
TTFB row above tracks the browser-observed value, which includes network
plus the client's render-blocking work.

## Per-route bundle ceiling

Each `(app)` route's incremental client JS bundle should stay under
**250KB compressed** (uncompressed roughly 750KB). This is a soft target
and is not gated. Operators may run `pnpm build` and inspect the
`.next/static/chunks` listing for the per-route delta. Aggressive
violations (e.g. doubling the budget on a single route) should be raised
as a regression even before CI gating exists.

## How to measure (operator)

Three options, listed from cheapest to most thorough:

- **(a) Live measurement.** Scrape `/api/metrics` from the running
  frontend. The histogram families `wh_frontend_web_vital_seconds`
  (LCP, INP, FCP, TTFB) and `wh_frontend_web_vital_score` (CLS) carry per-route
  distributions tagged with `name` and `route` labels. Use Grafana or
  `promtool` to compute p75 / p95 against the targets above. A reasonable
  alert rule is: notify if route p95 LCP exceeds 2.5s for more than 1h.
- **(b) Synthetic measurement.** Run `pnpm dev`, open browser devtools'
  Performance tab, and capture LCP / CLS / INP per route by hand. Useful
  for one-off investigation; not a substitute for real-user data.
- **(c) Lighthouse CLI (operator-side).**
  `npx lighthouse http://localhost:3000/dashboard --only-categories=performance --form-factor=mobile`
  produces a one-shot performance score. Run manually; not currently wired
  into this repo's CI (see "Deferred" below).

## What we measure today

The five Core Web Vitals are recorded in-process; Next-specific events
(`Next.js-hydration`, `Next.js-route-change-to-render`, `Next.js-render`)
are filtered out at the client to keep histogram cardinality bounded.

| Component | Path |
| --- | --- |
| Client collector | `frontend/src/components/web-vitals-reporter.tsx` |
| Ingest endpoint | `frontend/src/app/api/vitals/route.ts` |
| Registry storage | `frontend/src/lib/metrics/registry.ts` (web vitals split into `web-vital-histogram.ts`) |
| Scrape surface | `/api/metrics` (existing, Phase 35) |

The collector mounts in the root layout; every authenticated and
unauthenticated route therefore reports. The route label is normalized via
the existing `normalizeRoute` helper so id-bearing paths
(e.g. `/exercises/abc123`) collapse to `/exercises/[id]` before recording.

## Deferred (NOT in v0.6)

The following pieces are intentionally postponed because they require edits
to protected paths. Each item has a clear unblock trigger.

- **Lighthouse CI integration**
  (`.github/workflows/lighthouse.yml` + `lighthouserc.json`).
  Trigger to unblock: operator approves a workflow add OR v1.0 hardening
  Phase 38+ explicitly takes ownership. Reason deferred:
  `.github/workflows/**` is on the protected-files list.
- **Bundle-size enforcement script**
  (`scripts/check-bundle-size.mjs`). Trigger: operator approves a
  `scripts/` add OR Phase 38+. Reason deferred: `scripts/` is a
  protected directory.
- **Per-route bundle reporting via `@next/bundle-analyzer`.**
  Trigger: a `package.json` devDependency add is approved.
  Reason deferred: `package.json` is protected.
- **CI gating that fails the build on regression.** Trigger: same as
  the items above. Reason deferred: depends on at least one of the
  protected-file edits being authorized.

## Owner

Repo maintainer.

## Related

- `docs/SELF_HOSTED_CONTRACT.md` — the operational contract this app obeys.
- `.planning/ISSUES.md` — deferred-work tracker, including the entries
  matching the bullets above.
- `frontend/src/lib/metrics/registry.ts` — process-local Prometheus
  registry; the web-vital recording lives behind `recordWebVital`.
- `frontend/src/app/api/vitals/route.ts` — POST ingest endpoint.
- `frontend/src/components/web-vitals-reporter.tsx` — client collector.
