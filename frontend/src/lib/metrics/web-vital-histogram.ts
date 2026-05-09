/**
 * Web Vitals histogram storage helpers used by the metrics registry.
 *
 * Time-based vitals (LCP, INP, FCP, TTFB) reuse the standard seconds bucket
 * array; the unitless CLS metric needs its own bucket array tuned to web.dev's
 * good/needs-improvement/poor thresholds. Two histogram families are emitted
 * so the unit semantics stay legible to the operator's Prometheus scraper.
 */

import { HISTOGRAM_BUCKETS_SECONDS } from "./registry-buckets";

export type WebVitalName = "LCP" | "CLS" | "INP" | "FCP" | "TTFB";

export const CLS_BUCKETS = [0.05, 0.1, 0.15, 0.25, 0.5, 1.0] as const;

export interface WebVitalHistogramData {
  buckets: number[];
  sum: number;
  count: number;
}

export const WEB_VITAL_SECONDS_NAME = "wh_frontend_web_vital_seconds";
export const WEB_VITAL_SCORE_NAME = "wh_frontend_web_vital_score";

const WEB_VITAL_SECONDS_HELP =
  "Core Web Vitals time-based metrics observed in the browser via next/web-vitals.";
const WEB_VITAL_SCORE_HELP =
  "Cumulative Layout Shift (unitless) observed in the browser.";

function bucketsFor(name: WebVitalName): readonly number[] {
  return name === "CLS" ? CLS_BUCKETS : HISTOGRAM_BUCKETS_SECONDS;
}

function newHistogram(name: WebVitalName): WebVitalHistogramData {
  return {
    buckets: new Array(bucketsFor(name).length).fill(0),
    sum: 0,
    count: 0,
  };
}

export function webVitalKey(name: WebVitalName, route: string): string {
  return `${name}|${route}`;
}

function escapeLabel(value: string): string {
  return value.replace(/\\/g, "\\\\").replace(/"/g, '\\"').replace(/\n/g, "\\n");
}

export function recordWebVitalInto(
  state: Map<string, WebVitalHistogramData>,
  name: WebVitalName,
  route: string,
  value: number,
): void {
  const key = webVitalKey(name, route);
  let hist = state.get(key);
  if (!hist) {
    hist = newHistogram(name);
    state.set(key, hist);
  }
  hist.sum += value;
  hist.count += 1;
  const buckets = bucketsFor(name);
  for (let i = 0; i < buckets.length; i += 1) {
    if (value <= buckets[i]) {
      hist.buckets[i] += 1;
    }
  }
}

function renderFamily(
  out: string[],
  metricName: string,
  help: string,
  state: Map<string, WebVitalHistogramData>,
  filter: (name: WebVitalName) => boolean,
  buckets: readonly number[],
): void {
  out.push(`# HELP ${metricName} ${help}`);
  out.push(`# TYPE ${metricName} histogram`);
  for (const [key, hist] of state) {
    const [name, route] = key.split("|") as [WebVitalName, string];
    if (!filter(name)) continue;
    const labelPrefix = `name="${escapeLabel(name)}",route="${escapeLabel(route)}"`;
    for (let i = 0; i < buckets.length; i += 1) {
      out.push(
        `${metricName}_bucket{${labelPrefix},le="${buckets[i]}"} ${hist.buckets[i]}`,
      );
    }
    out.push(`${metricName}_bucket{${labelPrefix},le="+Inf"} ${hist.count}`);
    out.push(`${metricName}_sum{${labelPrefix}} ${hist.sum}`);
    out.push(`${metricName}_count{${labelPrefix}} ${hist.count}`);
  }
}

export function renderWebVitals(
  state: Map<string, WebVitalHistogramData>,
): string[] {
  const out: string[] = [];
  renderFamily(
    out,
    WEB_VITAL_SECONDS_NAME,
    WEB_VITAL_SECONDS_HELP,
    state,
    (n) => n !== "CLS",
    HISTOGRAM_BUCKETS_SECONDS,
  );
  renderFamily(
    out,
    WEB_VITAL_SCORE_NAME,
    WEB_VITAL_SCORE_HELP,
    state,
    (n) => n === "CLS",
    CLS_BUCKETS,
  );
  return out;
}
