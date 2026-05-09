import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import { NextIntlClientProvider } from "next-intl";
import type { ReactElement, ReactNode } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: vi.fn(), replace: vi.fn(), refresh: vi.fn() }),
  useSearchParams: () => new URLSearchParams(),
  usePathname: () => "/exercises/x",
}));

vi.mock("recharts", async () => {
  const actual = await vi.importActual<typeof import("recharts")>("recharts");
  return {
    ...actual,
    ResponsiveContainer: ({ children }: { children: ReactNode }) => (
      <div style={{ width: 600, height: 300 }} data-testid="recharts-container">
        {children}
      </div>
    ),
  };
});

import { ExerciseProgressChart } from "@/components/exercise-progress-chart";
import { clearTokens } from "@/lib/auth/token-store";

const enMessages = {
  exercises: {
    progressChartTitle: "Progress",
    progressChartEmpty: "No progression yet",
    progressChartLoading: "Loading...",
    progressChartTooltipOneRm: "Estimated 1RM: {value} kg",
    progressChartTooltipMaxWeight: "Max weight: {value} kg",
    progressChartTooltipVolume: "Volume: {value} kg",
    progressChartTooltipSets: "Sets: {count}",
    progressChartTooltipReps: "Top reps: {count}",
  },
};

const trMessages = {
  exercises: {
    progressChartTitle: "Ilerleme",
    progressChartEmpty: "Henuz ilerleme verisi yok",
    progressChartLoading: "Yukleniyor...",
    progressChartTooltipOneRm: "Tahmini 1RM: {value} kg",
    progressChartTooltipMaxWeight: "En agir: {value} kg",
    progressChartTooltipVolume: "Hacim: {value} kg",
    progressChartTooltipSets: "Set: {count}",
    progressChartTooltipReps: "En cok tekrar: {count}",
  },
};

function renderClient(
  ui: ReactElement,
  locale: "en" | "tr" = "en"
): ReturnType<typeof render> {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return render(
    <NextIntlClientProvider
      locale={locale}
      messages={locale === "tr" ? trMessages : enMessages}
    >
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

const EXERCISE_ID = "ffffffff-1111-1111-1111-111111111111";

function progressPoint(overrides: Partial<{
  sessionId: string;
  startedAt: string;
  setCount: number;
  totalVolumeKg: number;
  maxWeightKg: number | null;
  topRepsDone: number;
  estimatedOneRmKg: number | null;
}>) {
  return {
    sessionId: "11111111-1111-1111-1111-111111111111",
    startedAt: "2026-04-01T12:00:00Z",
    setCount: 3,
    totalVolumeKg: 1500,
    maxWeightKg: 80,
    topRepsDone: 8,
    estimatedOneRmKg: 92.5,
    ...overrides,
  };
}

describe("ExerciseProgressChart", () => {
  beforeEach(() => {
    clearTokens();
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  it("renders the chart heading and a chart when progress points exist", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async () =>
        jsonResponse(200, [
          progressPoint({
            sessionId: "11111111-1111-1111-1111-111111111111",
            startedAt: "2026-03-01T12:00:00Z",
          }),
          progressPoint({
            sessionId: "22222222-2222-2222-2222-222222222222",
            startedAt: "2026-03-08T12:00:00Z",
            estimatedOneRmKg: 95,
          }),
          progressPoint({
            sessionId: "33333333-3333-3333-3333-333333333333",
            startedAt: "2026-03-15T12:00:00Z",
            estimatedOneRmKg: 100,
          }),
        ])
      )
    );

    renderClient(<ExerciseProgressChart exerciseId={EXERCISE_ID} />);

    await waitFor(() =>
      expect(screen.getByText("Progress")).toBeInTheDocument()
    );
    await waitFor(() =>
      expect(screen.getByTestId("recharts-container")).toBeInTheDocument()
    );
  });

  it("renders the empty-state copy when the endpoint returns []", async () => {
    vi.stubGlobal("fetch", vi.fn(async () => jsonResponse(200, [])));

    renderClient(<ExerciseProgressChart exerciseId={EXERCISE_ID} />);

    await waitFor(() =>
      expect(screen.getByText("No progression yet")).toBeInTheDocument()
    );
  });

  it("renders the loading copy during the initial fetch", async () => {
    let resolve: ((res: Response) => void) | null = null;
    const pending = new Promise<Response>((r) => {
      resolve = r;
    });
    vi.stubGlobal("fetch", vi.fn(() => pending));

    renderClient(<ExerciseProgressChart exerciseId={EXERCISE_ID} />);

    expect(screen.getByText("Loading...")).toBeInTheDocument();
    resolve?.(jsonResponse(200, []));
    await waitFor(() =>
      expect(screen.getByText("No progression yet")).toBeInTheDocument()
    );
  });

  it("falls back to the empty-state copy on a 5xx response", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async () =>
        jsonResponse(500, {
          timestamp: "2026-04-01T12:00:00Z",
          status: 500,
          error: "Internal Server Error",
          message: "boom",
          path: `/api/exercises/${EXERCISE_ID}/progress`,
        })
      )
    );

    renderClient(<ExerciseProgressChart exerciseId={EXERCISE_ID} />);

    await waitFor(() =>
      expect(screen.getByText("No progression yet")).toBeInTheDocument()
    );
  });

  it("uses the active locale for the Turkish path render", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async () =>
        jsonResponse(200, [
          progressPoint({ startedAt: "2026-03-01T12:00:00Z" }),
        ])
      )
    );

    renderClient(
      <ExerciseProgressChart exerciseId={EXERCISE_ID} />,
      "tr"
    );

    await waitFor(() =>
      expect(screen.getByText("Ilerleme")).toBeInTheDocument()
    );
  });
});
