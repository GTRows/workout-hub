/**
 * Process-local Prometheus metrics registry.
 *
 * Hand-rolled (no prom-client) because the contract requires a small fixed set
 * of HTTP metrics on a single-instance frontend. The registry is mounted on
 * `globalThis` so the Next.js middleware (writer) and the `/api/metrics` route
 * handler (reader) share state regardless of which Next runtime adapter is
 * active. Counter resets on process restart are accepted per the standard
 * Prometheus contract; the operator scrape sees a fresh `process_start_time`.
 */

export type StatusClass = "1xx" | "2xx" | "3xx" | "4xx" | "5xx";

export interface MetricsRegistry {
  record(method: string, route: string, status: number, durationMs: number): void;
  renderProm(): string;
  reset(): void;
}

export const HISTOGRAM_BUCKETS_SECONDS = [
  0.005, 0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 1, 2.5, 5, 10,
] as const;

interface HistogramData {
  buckets: number[];
  sum: number;
  count: number;
}

interface RegistryState {
  counters: Map<string, number>;
  histograms: Map<string, HistogramData>;
}

const COUNTER_HELP =
  "Total HTTP requests observed by the Next.js middleware, classified by method, normalized route, and status class. status_class is the middleware outcome (pass-through vs redirect), not the upstream route handler outcome.";
const COUNTER_TYPE = "counter";
const HISTOGRAM_HELP =
  "HTTP request duration in seconds as observed by the Next.js middleware. Captures middleware processing time; upstream route handler latency is not included.";
const HISTOGRAM_TYPE = "histogram";

const COUNTER_NAME = "wh_frontend_http_requests_total";
const HISTOGRAM_NAME = "wh_frontend_http_request_duration_seconds";

function statusClass(status: number): StatusClass {
  if (status >= 500) return "5xx";
  if (status >= 400) return "4xx";
  if (status >= 300) return "3xx";
  if (status >= 200) return "2xx";
  return "1xx";
}

function counterKey(method: string, route: string, sc: StatusClass): string {
  return `${method}|${route}|${sc}`;
}

function histogramKey(method: string, route: string): string {
  return `${method}|${route}`;
}

function newHistogram(): HistogramData {
  return {
    buckets: new Array(HISTOGRAM_BUCKETS_SECONDS.length).fill(0),
    sum: 0,
    count: 0,
  };
}

function escapeLabel(value: string): string {
  return value.replace(/\\/g, "\\\\").replace(/"/g, '\\"').replace(/\n/g, "\\n");
}

function createRegistry(): MetricsRegistry {
  const state: RegistryState = {
    counters: new Map(),
    histograms: new Map(),
  };

  function record(method: string, route: string, status: number, durationMs: number): void {
    const sc = statusClass(status);
    const cKey = counterKey(method, route, sc);
    state.counters.set(cKey, (state.counters.get(cKey) ?? 0) + 1);

    const hKey = histogramKey(method, route);
    let hist = state.histograms.get(hKey);
    if (!hist) {
      hist = newHistogram();
      state.histograms.set(hKey, hist);
    }
    const seconds = durationMs / 1000;
    hist.sum += seconds;
    hist.count += 1;
    for (let i = 0; i < HISTOGRAM_BUCKETS_SECONDS.length; i += 1) {
      if (seconds <= HISTOGRAM_BUCKETS_SECONDS[i]) {
        hist.buckets[i] += 1;
      }
    }
  }

  function renderProm(): string {
    const out: string[] = [];

    out.push(`# HELP ${COUNTER_NAME} ${COUNTER_HELP}`);
    out.push(`# TYPE ${COUNTER_NAME} ${COUNTER_TYPE}`);
    for (const [key, value] of state.counters) {
      const [method, route, sc] = key.split("|");
      out.push(
        `${COUNTER_NAME}{method="${escapeLabel(method)}",route="${escapeLabel(route)}",status_class="${sc}"} ${value}`,
      );
    }

    out.push(`# HELP ${HISTOGRAM_NAME} ${HISTOGRAM_HELP}`);
    out.push(`# TYPE ${HISTOGRAM_NAME} ${HISTOGRAM_TYPE}`);
    for (const [key, hist] of state.histograms) {
      const [method, route] = key.split("|");
      const labelPrefix = `method="${escapeLabel(method)}",route="${escapeLabel(route)}"`;
      for (let i = 0; i < HISTOGRAM_BUCKETS_SECONDS.length; i += 1) {
        out.push(
          `${HISTOGRAM_NAME}_bucket{${labelPrefix},le="${HISTOGRAM_BUCKETS_SECONDS[i]}"} ${hist.buckets[i]}`,
        );
      }
      out.push(`${HISTOGRAM_NAME}_bucket{${labelPrefix},le="+Inf"} ${hist.count}`);
      out.push(`${HISTOGRAM_NAME}_sum{${labelPrefix}} ${hist.sum}`);
      out.push(`${HISTOGRAM_NAME}_count{${labelPrefix}} ${hist.count}`);
    }

    return out.join("\n") + "\n";
  }

  function reset(): void {
    state.counters.clear();
    state.histograms.clear();
  }

  return { record, renderProm, reset };
}

interface GlobalWithRegistry {
  __wh_metrics?: MetricsRegistry;
}

export function getRegistry(): MetricsRegistry {
  const g = globalThis as GlobalWithRegistry;
  if (!g.__wh_metrics) {
    g.__wh_metrics = createRegistry();
  }
  return g.__wh_metrics;
}
