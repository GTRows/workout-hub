import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import { NextIntlClientProvider } from "next-intl";
import type { ReactElement } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

// Pin the ISO-week range to a known window so tests are deterministic.
const PINNED_START = new Date(2026, 4, 4, 0, 0, 0, 0);
const PINNED_END = new Date(2026, 4, 10, 23, 59, 59, 999);
vi.mock("@/lib/time/iso-week", async () => {
  const actual = await vi.importActual<typeof import("@/lib/time/iso-week")>(
    "@/lib/time/iso-week"
  );
  return {
    ...actual,
    getCurrentIsoWeekRange: () => ({ start: PINNED_START, end: PINNED_END }),
  };
});

import { WeeklySummaryCard } from "@/components/dashboard/weekly-summary-card";

const messages = {
  dashboard: {
    loading: "Loading...",
    weeklySummaryTitle: "This week",
    weeklySummaryCount: "{completed}/{total} workouts",
    weeklySummaryTarget: "{actual}/{target} target",
    weeklySummaryEmpty: "No workouts this week",
  },
};

function renderCard(ui: ReactElement) {
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

function routeFetch(routes: Record<string, () => Response>) {
  return vi.fn(async (input: string | URL | Request) => {
    const url = typeof input === "string" ? input : input.toString();
    for (const [suffix, factory] of Object.entries(routes)) {
      if (url.includes(suffix)) return factory();
    }
    throw new Error("unexpected fetch: " + url);
  });
}

function makeSession(
  id: string,
  startedAt: Date,
  finished: boolean
): {
  id: string;
  workoutDayId: null;
  startedAt: string;
  endedAt: string | null;
  finished: boolean;
  setCount: number;
} {
  return {
    id,
    workoutDayId: null,
    startedAt: startedAt.toISOString(),
    endedAt: finished ? startedAt.toISOString() : null,
    finished,
    setCount: 0,
  };
}

const planFixture = {
  id: "11111111-1111-1111-1111-111111111111",
  name: "Test Plan",
  active: true,
  createdAt: "2026-04-01T00:00:00Z",
  updatedAt: "2026-04-01T00:00:00Z",
  days: [
    {
      id: "22222222-2222-2222-2222-222222222222",
      dayOfWeek: 1,
      name: "Mon",
      focus: "PUSH",
      exercises: [],
    },
    {
      id: "33333333-3333-3333-3333-333333333333",
      dayOfWeek: 3,
      name: "Wed",
      focus: "PULL",
      exercises: [],
    },
    {
      id: "44444444-4444-4444-4444-444444444444",
      dayOfWeek: 5,
      name: "Fri",
      focus: "LEGS",
      exercises: [],
    },
  ],
};

describe("WeeklySummaryCard", () => {
  beforeEach(() => {
    // no-op
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("renders count when sessions exist this week", async () => {
    const sessions = [
      makeSession(
        "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa01",
        new Date(2026, 4, 4, 10, 0, 0),
        true
      ),
      makeSession(
        "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa02",
        new Date(2026, 4, 6, 10, 0, 0),
        true
      ),
      makeSession(
        "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa03",
        new Date(2026, 4, 8, 10, 0, 0),
        false
      ),
    ];
    vi.stubGlobal(
      "fetch",
      routeFetch({
        "/api/workout-plans/active": () => noContent(),
        "/api/sessions/history": () =>
          jsonResponse(200, {
            content: sessions,
            totalElements: sessions.length,
            totalPages: 1,
            number: 0,
            size: 50,
          }),
      })
    );

    renderCard(<WeeklySummaryCard />);
    expect(await screen.findByText("2/3 workouts")).toBeInTheDocument();
  });

  it("renders empty copy when zero this-week sessions exist", async () => {
    vi.stubGlobal(
      "fetch",
      routeFetch({
        "/api/workout-plans/active": () => noContent(),
        "/api/sessions/history": () =>
          jsonResponse(200, {
            content: [],
            totalElements: 0,
            totalPages: 0,
            number: 0,
            size: 50,
          }),
      })
    );

    renderCard(<WeeklySummaryCard />);
    expect(
      await screen.findByText("No workouts this week")
    ).toBeInTheDocument();
  });

  it("hides target line when no active plan", async () => {
    const sessions = [
      makeSession(
        "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa01",
        new Date(2026, 4, 4, 10, 0, 0),
        true
      ),
      makeSession(
        "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa02",
        new Date(2026, 4, 6, 10, 0, 0),
        false
      ),
    ];
    vi.stubGlobal(
      "fetch",
      routeFetch({
        "/api/workout-plans/active": () => noContent(),
        "/api/sessions/history": () =>
          jsonResponse(200, {
            content: sessions,
            totalElements: sessions.length,
            totalPages: 1,
            number: 0,
            size: 50,
          }),
      })
    );

    renderCard(<WeeklySummaryCard />);
    expect(await screen.findByText("1/2 workouts")).toBeInTheDocument();
    expect(screen.queryByText(/target/i)).not.toBeInTheDocument();
  });

  it("respects the ISO-week filter", async () => {
    const sessions = [
      makeSession(
        "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa01",
        new Date(2026, 4, 5, 10, 0, 0),
        true
      ),
      makeSession(
        "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa02",
        new Date(2026, 4, 6, 10, 0, 0),
        true
      ),
      makeSession(
        "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa03",
        new Date(2026, 3, 25, 10, 0, 0),
        true
      ),
      makeSession(
        "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa04",
        new Date(2026, 4, 12, 10, 0, 0),
        true
      ),
    ];
    vi.stubGlobal(
      "fetch",
      routeFetch({
        "/api/workout-plans/active": () => jsonResponse(200, planFixture),
        "/api/sessions/history": () =>
          jsonResponse(200, {
            content: sessions,
            totalElements: sessions.length,
            totalPages: 1,
            number: 0,
            size: 50,
          }),
      })
    );

    renderCard(<WeeklySummaryCard />);
    expect(await screen.findByText("2/2 workouts")).toBeInTheDocument();
    expect(screen.getByText("2/3 target")).toBeInTheDocument();
  });
});
