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
    weightTitle: "Weight trend",
    weightDescription: "Recent weight change.",
    weightCurrent: "Current: {value} kg",
    weightDelta: "Change: {value} kg",
    weightDeltaEmpty: "Not enough entries yet.",
    weightRange: { seven: "7 days", thirty: "30 days", all: "All" },
    range: {
      fourWeeks: "4 weeks",
      twelveWeeks: "12 weeks",
      twentySixWeeks: "26 weeks",
    },
    volumeTooltipLabel: "Total volume (kg)",
    oneRmTooltipLabel: "1RM (kg)",
    prTeaserTitle: "Recent PRs",
    prTeaserViewAll: "View all",
  },
  prs: {
    empty: "No records yet.",
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
        if (url.endsWith("/api/metrics")) {
          return jsonResponse(200, [
            {
              id: "00000000-0000-0000-0000-000000000001",
              recordedDate: "2026-04-01",
              weightKg: 80.5,
              createdAt: "2026-04-01T00:00:00Z",
              updatedAt: "2026-04-01T00:00:00Z",
            },
            {
              id: "00000000-0000-0000-0000-000000000002",
              recordedDate: "2026-04-15",
              weightKg: 79.8,
              createdAt: "2026-04-15T00:00:00Z",
              updatedAt: "2026-04-15T00:00:00Z",
            },
            {
              id: "00000000-0000-0000-0000-000000000003",
              recordedDate: "2026-04-22",
              weightKg: 79.0,
              createdAt: "2026-04-22T00:00:00Z",
              updatedAt: "2026-04-22T00:00:00Z",
            },
          ]);
        }
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
        if (url.endsWith("/api/metrics")) return jsonResponse(200, []);
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

  it("renders the weight trend card with current and delta when metrics are populated", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async (input: string | URL | Request) => {
        const url = typeof input === "string" ? input : input.toString();
        if (url.endsWith("/api/metrics")) {
          const today = new Date();
          const earlier = new Date(today.getTime() - 20 * 24 * 60 * 60 * 1000);
          return jsonResponse(200, [
            {
              id: "00000000-0000-0000-0000-000000000010",
              recordedDate: earlier.toISOString().slice(0, 10),
              weightKg: 82.0,
              createdAt: earlier.toISOString(),
              updatedAt: earlier.toISOString(),
            },
            {
              id: "00000000-0000-0000-0000-000000000011",
              recordedDate: today.toISOString().slice(0, 10),
              weightKg: 80.0,
              createdAt: today.toISOString(),
              updatedAt: today.toISOString(),
            },
          ]);
        }
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
    expect(await screen.findByTestId("weight-current")).toHaveTextContent("80");
    expect(screen.getByTestId("weight-delta")).toHaveTextContent("-2.0");
  });

  it("shows the weight delta empty state when there are fewer than 2 metrics", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async (input: string | URL | Request) => {
        const url = typeof input === "string" ? input : input.toString();
        if (url.endsWith("/api/metrics")) return jsonResponse(200, []);
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
    expect(await screen.findByTestId("weight-delta-empty")).toBeInTheDocument();
  });

  it("renders the PR teaser card with up to 3 most-recent PRs and a view-all link", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async (input: string | URL | Request) => {
        const url = typeof input === "string" ? input : input.toString();
        if (url.endsWith("/api/metrics")) return jsonResponse(200, []);
        if (url.includes("/api/analytics/volume")) return jsonResponse(200, []);
        if (url.endsWith("/api/analytics/prs")) {
          return jsonResponse(200, [
            {
              exerciseId: "00000000-0000-0000-0000-0000000000a1",
              exerciseNameTr: "Bench",
              exerciseNameEn: "Bench",
              estimatedOneRmKg: 100,
              weightKg: 80,
              repsDone: 5,
              achievedAt: "2026-04-01",
            },
            {
              exerciseId: "00000000-0000-0000-0000-0000000000e2",
              exerciseNameTr: "Squat",
              exerciseNameEn: "Squat",
              estimatedOneRmKg: 130,
              weightKg: 110,
              repsDone: 5,
              achievedAt: "2026-04-22",
            },
            {
              exerciseId: "00000000-0000-0000-0000-0000000000e3",
              exerciseNameTr: "Deadlift",
              exerciseNameEn: "Deadlift",
              estimatedOneRmKg: 160,
              weightKg: 140,
              repsDone: 5,
              achievedAt: "2026-04-15",
            },
            {
              exerciseId: "00000000-0000-0000-0000-0000000000e4",
              exerciseNameTr: "Press",
              exerciseNameEn: "Press",
              estimatedOneRmKg: 70,
              weightKg: 60,
              repsDone: 5,
              achievedAt: "2026-03-10",
            },
          ]);
        }
        if (url.endsWith("/api/analytics/streak")) {
          return jsonResponse(200, {
            currentStreakDays: 0,
            longestStreakDays: 0,
            lastSessionDate: null,
          });
        }
        if (url.includes("/api/analytics/heatmap")) return jsonResponse(200, []);
        if (url.includes("/api/analytics/one-rm/")) return jsonResponse(200, []);
        throw new Error("unexpected fetch: " + url);
      })
    );

    renderClient(<InsightsClient />);
    await screen.findByTestId(
      "pr-teaser-row-00000000-0000-0000-0000-0000000000e2"
    );
    expect(
      screen.getByTestId("pr-teaser-row-00000000-0000-0000-0000-0000000000e3")
    ).toBeInTheDocument();
    expect(
      screen.getByTestId("pr-teaser-row-00000000-0000-0000-0000-0000000000a1")
    ).toBeInTheDocument();
    expect(
      screen.queryByTestId("pr-teaser-row-00000000-0000-0000-0000-0000000000e4")
    ).toBeNull();
    const viewAll = screen.getByTestId("pr-teaser-view-all");
    expect(viewAll.getAttribute("href")).toBe("/prs");
  });
});
