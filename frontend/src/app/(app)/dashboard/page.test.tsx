import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import { NextIntlClientProvider } from "next-intl";
import type { ReactElement } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

const pushMock = vi.fn();
vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: pushMock, replace: vi.fn(), refresh: vi.fn() }),
  useSearchParams: () => new URLSearchParams(),
  usePathname: () => "/dashboard",
}));

// Pin the ISO day-of-week so tests are deterministic.
vi.mock("@/lib/time/today", () => ({
  getTodayIsoDayOfWeek: () => 3,
}));

import DashboardPage from "@/app/(app)/dashboard/page";
import { clearTokens } from "@/lib/auth/token-store";

const messages = {
  dashboard: {
    loading: "Loading...",
    resumeTitle: "Active session",
    resumeHint: "You have a workout in progress",
    resumeButton: "Resume",
    todayLabel: "Today",
    startButton: "Start workout",
    exerciseCount: "{count} exercises",
    restDay: "Rest day",
    noPlan: "No active plan",
    quickAdd: "Log weight",
    viewPlan: "View plan",
    weeklySummaryTitle: "This week",
    weeklySummaryCount: "{completed}/{total} workouts",
    weeklySummaryTarget: "{actual}/{target} target",
    weeklySummaryEmpty: "No workouts this week",
    lastWeightTitle: "Last weight",
    lastWeightValue: "{kg} kg",
    lastWeightToday: "Today",
    lastWeightDaysAgo: "{days} days ago",
    lastWeightAbsolute: "{date}",
    lastWeightEmpty: "No weight recorded yet",
    lastWeightAddCta: "Log weight",
    viewHistory: "History",
  },
};

function renderDashboard(ui: ReactElement) {
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

function noContent(): Response {
  return new Response(null, { status: 204 });
}

const activePlan = {
  id: "11111111-1111-1111-1111-111111111111",
  name: "Test Plan",
  active: true,
  createdAt: "2026-04-01T00:00:00Z",
  updatedAt: "2026-04-01T00:00:00Z",
  days: [
    {
      id: "22222222-2222-2222-2222-222222222222",
      dayOfWeek: 3,
      name: "Mid-week Push",
      focus: "push",
      estimatedDurationMin: 45,
      exercises: [
        {
          id: "33333333-3333-3333-3333-333333333333",
          exerciseId: "44444444-4444-4444-4444-444444444444",
          exerciseNameTr: "Sinav",
          exerciseNameEn: "Push-up",
          orderIndex: 1,
          targetSets: 3,
          targetRepsMin: 8,
          targetRepsMax: 12,
        },
        {
          id: "55555555-5555-5555-5555-555555555555",
          exerciseId: "66666666-6666-6666-6666-666666666666",
          exerciseNameTr: "Omuz Baski",
          exerciseNameEn: "Shoulder Press",
          orderIndex: 2,
          targetSets: 3,
        },
      ],
    },
  ],
};

function routeFetch(routes: Record<string, () => Response>) {
  return vi.fn(async (input: string | URL | Request) => {
    const url = typeof input === "string" ? input : input.toString();
    for (const [suffix, factory] of Object.entries(routes)) {
      if (url.includes(suffix)) return factory();
    }
    throw new Error("unexpected fetch: " + url);
  });
}

const emptyHistoryPage = {
  content: [],
  totalElements: 0,
  totalPages: 0,
  number: 0,
  size: 50,
};

const dashboardSideRoutes = {
  "/api/sessions/history": () => jsonResponse(200, emptyHistoryPage),
  "/api/metrics": () => jsonResponse(200, []),
};

describe("DashboardPage", () => {
  beforeEach(() => {
    clearTokens();
    window.localStorage.clear();
    pushMock.mockReset();
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("renders Resume when an active session exists", async () => {
    vi.stubGlobal(
      "fetch",
      routeFetch({
        "/api/workout-plans/active": () => jsonResponse(200, activePlan),
        "/api/sessions/active": () =>
          jsonResponse(200, {
            id: "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
            workoutDayId: null,
            startedAt: "2026-04-23T10:00:00Z",
            endedAt: null,
            finished: false,
            sets: [],
          }),
        ...dashboardSideRoutes,
      })
    );

    renderDashboard(<DashboardPage />);
    expect(await screen.findByRole("button", { name: /resume/i })).toBeInTheDocument();
  });

  it("renders Start when today's day is in the active plan", async () => {
    vi.stubGlobal(
      "fetch",
      routeFetch({
        "/api/workout-plans/active": () => jsonResponse(200, activePlan),
        "/api/sessions/active": () => noContent(),
        ...dashboardSideRoutes,
      })
    );

    renderDashboard(<DashboardPage />);
    expect(await screen.findByText("Mid-week Push")).toBeInTheDocument();
    expect(screen.getByText(/2 exercises/i)).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /start workout/i })).toBeInTheDocument();
  });

  it("renders Rest day when today is not in the plan", async () => {
    const planNoToday = {
      ...activePlan,
      days: [{ ...activePlan.days[0], dayOfWeek: 1 }],
    };
    vi.stubGlobal(
      "fetch",
      routeFetch({
        "/api/workout-plans/active": () => jsonResponse(200, planNoToday),
        "/api/sessions/active": () => noContent(),
        ...dashboardSideRoutes,
      })
    );

    renderDashboard(<DashboardPage />);
    expect(await screen.findByText(/rest day/i)).toBeInTheDocument();
  });

  it("renders No plan message when no active plan exists", async () => {
    vi.stubGlobal(
      "fetch",
      routeFetch({
        "/api/workout-plans/active": () => noContent(),
        "/api/sessions/active": () => noContent(),
        ...dashboardSideRoutes,
      })
    );

    renderDashboard(<DashboardPage />);
    expect(await screen.findByText(/no active plan/i)).toBeInTheDocument();
  });

  it("starts a session and navigates to /session/{id} when Start is clicked", async () => {
    const user = (await import("@testing-library/user-event")).default.setup();
    const newSessionId = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";
    vi.stubGlobal(
      "fetch",
      routeFetch({
        "/api/workout-plans/active": () => jsonResponse(200, activePlan),
        "/api/sessions/active": () => noContent(),
        "/api/sessions/start": () =>
          jsonResponse(201, {
            id: newSessionId,
            workoutDayId: activePlan.days[0].id,
            startedAt: "2026-04-23T10:00:00Z",
            endedAt: null,
            finished: false,
            sets: [],
          }),
        ...dashboardSideRoutes,
      })
    );

    renderDashboard(<DashboardPage />);
    const button = await screen.findByRole("button", { name: /start workout/i });
    await user.click(button);

    await waitFor(() =>
      expect(pushMock).toHaveBeenCalledWith(`/session/${newSessionId}`)
    );
  });

  it("renders WeeklySummaryCard and LastWeightCard in the Start branch", async () => {
    const recentDate = new Date();
    recentDate.setHours(0, 0, 0, 0);
    recentDate.setDate(recentDate.getDate() - 2);
    const yyyy = recentDate.getFullYear();
    const mm = String(recentDate.getMonth() + 1).padStart(2, "0");
    const dd = String(recentDate.getDate()).padStart(2, "0");
    const recentIso = `${yyyy}-${mm}-${dd}`;

    vi.stubGlobal(
      "fetch",
      routeFetch({
        "/api/workout-plans/active": () => jsonResponse(200, activePlan),
        "/api/sessions/active": () => noContent(),
        "/api/sessions/history": () =>
          jsonResponse(200, {
            content: [
              {
                id: "cccccccc-cccc-cccc-cccc-cccccccccccc",
                workoutDayId: null,
                startedAt: new Date().toISOString(),
                endedAt: null,
                finished: true,
                setCount: 0,
              },
            ],
            totalElements: 1,
            totalPages: 1,
            number: 0,
            size: 50,
          }),
        "/api/metrics": () =>
          jsonResponse(200, [
            {
              id: "dddddddd-dddd-dddd-dddd-dddddddddddd",
              recordedDate: recentIso,
              weightKg: 75.5,
              bodyFatPercent: null,
              waistCm: null,
              chestCm: null,
              armCm: null,
              thighCm: null,
              photoUrl: null,
              notes: null,
              createdAt: "2026-04-01T00:00:00Z",
              updatedAt: "2026-04-01T00:00:00Z",
            },
          ]),
      })
    );

    renderDashboard(<DashboardPage />);
    expect(await screen.findByText("Mid-week Push")).toBeInTheDocument();
    expect(await screen.findByText(/this week/i)).toBeInTheDocument();
    expect(await screen.findByText("75.5 kg")).toBeInTheDocument();
  });

  it("includes a History link in QuickActions", async () => {
    vi.stubGlobal(
      "fetch",
      routeFetch({
        "/api/workout-plans/active": () => noContent(),
        "/api/sessions/active": () => noContent(),
        ...dashboardSideRoutes,
      })
    );

    renderDashboard(<DashboardPage />);
    const link = await screen.findByRole("link", { name: /history/i });
    expect(link).toHaveAttribute("href", "/history");
  });
});
