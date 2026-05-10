// Lighthouse CI config. Thresholds mirror docs/PERF_BUDGETS.md verbatim.
// Source of truth for the numbers: web.dev's published Web Vitals
// thresholds (https://web.dev/articles/vitals).
//
// Audited routes are the unauthenticated set: /login, /, /offline,
// /_not-found. Authenticated-route Lighthouse coverage requires a
// CI-side backend boot and is deferred to v1.2+ (see plan 49-01
// Out-of-Scope block).
module.exports = {
  ci: {
    collect: {
      // lhci autorun is invoked with cwd=frontend/ (see .github/workflows/lighthouse.yml),
      // so a plain `pnpm start` runs the frontend package's start script directly.
      // The previous --filter ./frontend resolved to frontend/frontend/ from that cwd
      // and never started the server, which surfaced as a Chrome interstitial.
      startServerCommand: "pnpm start",
      startServerReadyPattern: "Ready in",
      url: [
        "http://localhost:3000/login",
        "http://localhost:3000/",
        "http://localhost:3000/offline",
        "http://localhost:3000/_not-found",
      ],
      numberOfRuns: 3,
      settings: {
        preset: "desktop",
      },
    },
    assert: {
      assertions: {
        // Core Web Vitals (per-route p75 targets from docs/PERF_BUDGETS.md).
        "largest-contentful-paint": ["error", { maxNumericValue: 2500 }],
        "cumulative-layout-shift": ["error", { maxNumericValue: 0.1 }],
        "interaction-to-next-paint": ["warn", { maxNumericValue: 200 }],
        "first-contentful-paint": ["error", { maxNumericValue: 1800 }],
        "server-response-time": ["error", { maxNumericValue: 800 }],
        // Performance category floor.
        "categories:performance": ["error", { minScore: 0.9 }],
      },
    },
    upload: {
      target: "temporary-public-storage",
    },
  },
};
