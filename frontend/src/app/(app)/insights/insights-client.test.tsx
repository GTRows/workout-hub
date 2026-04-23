import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import { NextIntlClientProvider } from "next-intl";
import type { ReactElement } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: vi.fn(), replace: vi.fn(), refresh: vi.fn() }),
  useSearchParams: () => new URLSearchParams(),
  usePathname: () => "/insights",
}));

vi.mock("recharts", async () => {
  const actual = await vi.importActual<typeof import("recharts")>("recharts");
  return {
    ...actual,
    ResponsiveContainer: ({ children }: { children: React.ReactNode }) => (
      <div style={{ width: 600, height: 300 }} data-testid="recharts-container">
        {children}
      </div>
    ),
  };
});

import { InsightsClient } from "@/app/(app)/insights/insights-client";
import { clearTokens } from "@/lib/auth/token-store";

const messages = {
  insights: {
    title: "Insights",
    loading: "Loading...",
    streakTitle: "Streak",
    streakCurrent: "Current: {days} days",
    streakLongest: "Longest: {days} days",
    volumeTitle: "Weekly volume",
    volumeDescription: "Total weight x reps over the last 12 weeks.",
    oneRmTitle: "Estimated 1RM",
    oneRmDescription: "Epley formula.",
    oneRmEmpty: "Not enough data yet.",
    exerciseLabel: "Exercise",
  },
};

function sampleHeatmap(weeks = 12) {
  const out = [];
  const start = new Date("2026-02-09");
  for (let i = 0; i < weeks * 7; i++) {
    const d = new Date(start);
    d.setDate(start.getDate() + i);
    out.push({
      date: d.toISOString().slice(0, 10),
      sessionCount: i === weeks * 7 - 1 ? 1 : 0,
    });
  }
  return out;
}

function renderClient(ui: ReactElement) {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return render(
    <NextIntlClientProvider locale="en" messages={messages}>
      <QueryClientProvider client={client}>{ui}</QueryClientProvider>
    </NextIntlClientProvider>
  );
}

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "content-type": "application/json" },
  });
}

const exerciseA = "ffffffff-aaaa-aaaa-aaaa-aaaaaaaaaaaa";

describe("InsightsClient", () => {
  beforeEach(() => {
    clearTokens();
    window.localStorage.clear();
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  it("renders the volume and 1RM charts when analytics payloads are populated", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async (input: string | URL | Request) => {
        const url = typeof input === "string" ? input : input.toString();
        if (url.includes("/api/analytics/volume")) {
          return jsonResponse(200, [
            { weekStart: "2026-04-13", totalVolumeKg: 500, sessionCount: 1 },
            { weekStart: "2026-04-20", totalVolumeKg: 1000, sessionCount: 2 },
          ]);
        }
        if (url.endsWith("/api/analytics/prs")) {
          return jsonResponse(200, [
            {
              exerciseId: exerciseA,
              exerciseNameTr: "Bench",
              exerciseNameEn: "Bench",
              estimatedOneRmKg: 116.67,
              weightKg: 100,
              repsDone: 5,
              achievedAt: "2026-04-20",
            },
          ]);
        }
        if (url.endsWith("/api/analytics/streak")) {
          return jsonResponse(200, {
            currentStreakDays: 3,
            longestStreakDays: 7,
            lastSessionDate: "2026-04-23",
          });
        }
        if (url.includes("/api/analytics/heatmap")) {
          return jsonResponse(200, sampleHeatmap(12));
        }
        if (url.includes("/api/analytics/one-rm/")) {
          return jsonResponse(200, [
            {
              date: "2026-04-10",
              estimatedOneRmKg: 110,
              repsDone: 5,
              weightKg: 95,
            },
            {
              date: "2026-04-20",
              estimatedOneRmKg: 116.67,
              repsDone: 5,
              weightKg: 100,
            },
          ]);
        }
        throw new Error("unexpected fetch: " + url);
      })
    );

    renderClient(<InsightsClient />);

    await waitFor(() => {
      expect(screen.getByTestId("streak-current")).toHaveTextContent("Current: 3 days");
    });
    expect(screen.getByTestId("streak-longest")).toHaveTextContent("Longest: 7 days");
    expect(screen.getByTestId("heatmap-grid")).toBeInTheDocument();

    const containers = await waitFor(() => {
      const found = screen.queryAllByTestId("recharts-container");
      expect(found.length).toBeGreaterThanOrEqual(2);
      return found;
    });
    expect(containers.length).toBeGreaterThanOrEqual(2);
    expect(screen.getByTestId("volume-chart")).toBeInTheDocument();
    expect(screen.getByTestId("one-rm-chart")).toBeInTheDocument();
  });

  it("shows the empty state when there are no PRs", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async (input: string | URL | Request) => {
        const url = typeof input === "string" ? input : input.toString();
        if (url.includes("/api/analytics/volume")) return jsonResponse(200, []);
        if (url.endsWith("/api/analytics/prs")) return jsonResponse(200, []);
        if (url.endsWith("/api/analytics/streak")) {
          return jsonResponse(200, {
            currentStreakDays: 0,
            longestStreakDays: 0,
            lastSessionDate: null,
          });
        }
        if (url.includes("/api/analytics/heatmap")) return jsonResponse(200, []);
        throw new Error("unexpected fetch: " + url);
      })
    );

    renderClient(<InsightsClient />);
    await screen.findByText("Not enough data yet.");
  });
});
