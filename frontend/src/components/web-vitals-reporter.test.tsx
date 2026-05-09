import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { render, cleanup } from "@testing-library/react";

type Metric = {
  id: string;
  name: string;
  value: number;
  navigationType?: string;
};

let pendingMetric: Metric | null = null;

vi.mock("next/web-vitals", () => ({
  useReportWebVitals: (cb: (m: Metric) => void) => {
    if (pendingMetric) cb(pendingMetric);
  },
}));

import { WebVitalsReporter } from "./web-vitals-reporter";

describe("<WebVitalsReporter />", () => {
  beforeEach(() => {
    pendingMetric = null;
    vi.restoreAllMocks();
  });

  afterEach(() => {
    cleanup();
  });

  it("posts via sendBeacon when available and returns null DOM", () => {
    const beacon = vi.fn().mockReturnValue(true);
    Object.defineProperty(globalThis.navigator, "sendBeacon", {
      configurable: true,
      writable: true,
      value: beacon,
    });
    pendingMetric = {
      id: "v3-1",
      name: "LCP",
      value: 1234,
      navigationType: "navigate",
    };
    const { container } = render(<WebVitalsReporter />);
    expect(container.firstChild).toBeNull();
    expect(beacon).toHaveBeenCalledTimes(1);
    expect(beacon.mock.calls[0][0]).toBe("/api/vitals");
    const blob = beacon.mock.calls[0][1] as Blob;
    expect(blob).toBeInstanceOf(Blob);
  });

  it("falls back to fetch with keepalive when sendBeacon is unavailable", async () => {
    delete (globalThis.navigator as unknown as { sendBeacon?: unknown })
      .sendBeacon;
    const fetchSpy = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValue(new Response(null, { status: 204 }));
    pendingMetric = {
      id: "v3-2",
      name: "INP",
      value: 150,
      navigationType: "navigate",
    };
    render(<WebVitalsReporter />);
    expect(fetchSpy).toHaveBeenCalledTimes(1);
    const [url, init] = fetchSpy.mock.calls[0];
    expect(url).toBe("/api/vitals");
    expect(init?.method).toBe("POST");
    expect(init?.keepalive).toBe(true);
  });

  it("filters out non-core Web Vitals events", () => {
    const beacon = vi.fn();
    Object.defineProperty(globalThis.navigator, "sendBeacon", {
      configurable: true,
      writable: true,
      value: beacon,
    });
    const fetchSpy = vi.spyOn(globalThis, "fetch");
    pendingMetric = {
      id: "next-h",
      name: "Next.js-hydration",
      value: 12,
    };
    render(<WebVitalsReporter />);
    expect(beacon).not.toHaveBeenCalled();
    expect(fetchSpy).not.toHaveBeenCalled();
  });

  it("passes CLS value through unchanged in the payload", async () => {
    const captured: { url?: string; payload?: string } = {};
    const beacon = vi.fn().mockImplementation((url: string, blob: Blob) => {
      captured.url = url;
      const buf = (blob as unknown as { _buffer?: string })._buffer;
      if (typeof buf === "string") {
        captured.payload = buf;
      }
      return true;
    });
    Object.defineProperty(globalThis.navigator, "sendBeacon", {
      configurable: true,
      writable: true,
      value: beacon,
    });
    const originalBlob = globalThis.Blob;
    class CapturingBlob {
      _buffer: string;
      type: string;
      constructor(parts: BlobPart[], options?: BlobPropertyBag) {
        this._buffer = parts.map((p) => String(p)).join("");
        this.type = options?.type ?? "";
      }
    }
    (globalThis as unknown as { Blob: typeof Blob }).Blob =
      CapturingBlob as unknown as typeof Blob;
    try {
      pendingMetric = {
        id: "v3-3",
        name: "CLS",
        value: 0.07,
        navigationType: "navigate",
      };
      render(<WebVitalsReporter />);
    } finally {
      (globalThis as unknown as { Blob: typeof Blob }).Blob = originalBlob;
    }
    expect(beacon).toHaveBeenCalledTimes(1);
    expect(captured.url).toBe("/api/vitals");
    expect(captured.payload).toBeDefined();
    const parsed = JSON.parse(captured.payload as string);
    expect(parsed.value).toBe(0.07);
    expect(parsed.name).toBe("CLS");
  });

  it("renders null (no DOM nodes)", () => {
    Object.defineProperty(globalThis.navigator, "sendBeacon", {
      configurable: true,
      writable: true,
      value: vi.fn().mockReturnValue(true),
    });
    pendingMetric = null;
    const { container } = render(<WebVitalsReporter />);
    expect(container.firstChild).toBeNull();
  });
});
