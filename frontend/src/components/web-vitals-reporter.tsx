"use client";

import { useReportWebVitals } from "next/web-vitals";

const ALLOWED = new Set(["LCP", "CLS", "INP", "FCP", "TTFB"]);

export function WebVitalsReporter() {
  useReportWebVitals((metric) => {
    if (!ALLOWED.has(metric.name)) return;
    const payload = JSON.stringify({
      name: metric.name,
      value: metric.value,
      id: metric.id,
      navigationType: metric.navigationType,
      route: typeof window !== "undefined" ? window.location.pathname : "/",
    });
    const url = "/api/vitals";
    if (typeof navigator !== "undefined" && typeof navigator.sendBeacon === "function") {
      const blob = new Blob([payload], { type: "application/json" });
      navigator.sendBeacon(url, blob);
      return;
    }
    void fetch(url, {
      method: "POST",
      body: payload,
      headers: { "content-type": "application/json" },
      keepalive: true,
    }).catch(() => {
      /* best-effort observability; drop on failure */
    });
  });
  return null;
}
