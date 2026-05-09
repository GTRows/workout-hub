import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { NextIntlClientProvider } from "next-intl";
import type { ReactElement } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

const pushMock = vi.fn();
vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: pushMock, replace: vi.fn(), refresh: vi.fn() }),
  useSearchParams: () => new URLSearchParams(),
  usePathname: () => "/history",
}));

import { HistoryClient } from "@/app/(app)/history/history-client";
import { clearTokens } from "@/lib/auth/token-store";

const messages = {
  history: {
    title: "History",
    loading: "Loading...",
    prevMonth: "Previous month",
    nextMonth: "Next month",
    today: "Today",
    noSession: "No session for this day",
    selectDayHint: "Tap a day",
    sessionTitle: "Session",
    sessionStartedAt: "Started: {time}",
    sessionFinishedAt: "Ended: {time}",
    setCount: "{count} sets",
    viewFullDetail: "View full detail",
    repeatWorkout: "Repeat workout",
    repeatWorkoutPending: "Starting...",
    weekday: {
      mon: "Mon",
      tue: "Tue",
      wed: "Wed",
      thu: "Thu",
      fri: "Fri",
      sat: "Sat",
      sun: "Sun",
    },
  },
  errors: {
    api: {
      generic: "Something went wrong. Please try again.",
      sessionAlreadyActive:
        "You already have an active session. Redirecting to resume.",
      sessionAlreadyFinished:
        "This session is already finished. Returning to dashboard.",
      sessionFinished:
        "Session was finished while you were submitting. Returning to dashboard.",
      setNumberDuplicate:
        "Set number conflict, list refreshed. Please try again.",
    },
  },
};

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

function summaryPage(items: unknown[]) {
  return {
    content: items,
    totalElements: items.length,
    totalPages: 1,
    number: 0,
    size: 200,
  };
}

describe("HistoryClient", () => {
  beforeEach(() => {
    clearTokens();
    window.localStorage.clear();
    pushMock.mockReset();
    // Fake timers pin the current date AND keep setTimeout working by
    // auto-advancing, so useEffect-based debouncing and RTL clicks behave.
    vi.useFakeTimers({ shouldAdvanceTime: true });
    vi.setSystemTime(new Date("2026-04-15T12:00:00Z"));
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.unstubAllGlobals();
  });

  it("highlights days that have a finished session", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async () =>
        jsonResponse(
          200,
          summaryPage([
            {
              id: "ffffffff-1111-1111-1111-111111111111",
              workoutDayId: null,
              startedAt: "2026-04-10T10:00:00Z",
              endedAt: "2026-04-10T11:00:00Z",
              finished: true,
              setCount: 9,
              mood: null,
              energyLevel: null,
            },
          ])
        )
      )
    );

    renderClient(<HistoryClient />);
    const dayButton = await screen.findByRole("button", { name: "2026-04-10" });
    await waitFor(() =>
      expect(dayButton.className).toContain("bg-primary/10")
    );
  });

  it("switches month on next/prev button", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => jsonResponse(200, summaryPage([])))
    );

    renderClient(<HistoryClient />);
    expect(await screen.findByText(/april 2026/i)).toBeInTheDocument();

    const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime });
    await user.click(screen.getByRole("button", { name: "Next month" }));
    expect(await screen.findByText(/may 2026/i)).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Previous month" }));
    expect(await screen.findByText(/april 2026/i)).toBeInTheDocument();
  });

  it("shows no-session message when tapping a day without history", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => jsonResponse(200, summaryPage([])))
    );

    renderClient(<HistoryClient />);
    const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime });
    const someDay = await screen.findByRole("button", { name: "2026-04-09" });
    await user.click(someDay);

    expect(await screen.findByText(/no session for this day/i)).toBeInTheDocument();
  });

  it("opens session detail card when tapping a day with a session", async () => {
    const sessionId = "ffffffff-2222-2222-2222-222222222222";
    vi.stubGlobal(
      "fetch",
      vi.fn(async (input: string | URL | Request) => {
        const url = typeof input === "string" ? input : input.toString();
        if (url.includes("/api/sessions/history")) {
          return jsonResponse(
            200,
            summaryPage([
              {
                id: sessionId,
                workoutDayId: null,
                startedAt: "2026-04-14T10:00:00Z",
                endedAt: "2026-04-14T11:00:00Z",
                finished: true,
                setCount: 12,
                mood: null,
                energyLevel: null,
              },
            ])
          );
        }
        if (url.endsWith(`/api/sessions/${sessionId}`)) {
          return jsonResponse(200, {
            id: sessionId,
            workoutDayId: null,
            startedAt: "2026-04-14T10:00:00Z",
            endedAt: "2026-04-14T11:00:00Z",
            finished: true,
            notes: null,
            mood: null,
            energyLevel: null,
            sets: [
              {
                id: "ffffffff-3333-3333-3333-333333333333",
                exerciseId: "ffffffff-4444-4444-4444-444444444444",
                exerciseNameTr: "Sinav",
                exerciseNameEn: "Push-up",
                setNumber: 1,
                repsDone: 10,
                weightKg: null,
                rpe: null,
                completed: true,
                notes: null,
              },
            ],
          });
        }
        throw new Error("unexpected fetch: " + url);
      })
    );

    renderClient(<HistoryClient />);
    const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime });
    const dayButton = await screen.findByRole("button", { name: "2026-04-14" });
    await user.click(dayButton);

    expect(await screen.findByText(/12 sets/)).toBeInTheDocument();
    expect(await screen.findByText(/Sinav/)).toBeInTheDocument();
  });

  it("renders Repeat-workout CTA only for sessions with a workoutDayId", async () => {
    const withDayId = "ffffffff-5555-5555-5555-555555555555";
    const noDayId = "ffffffff-6666-6666-6666-666666666666";
    vi.stubGlobal(
      "fetch",
      vi.fn(async (input: string | URL | Request) => {
        const url = typeof input === "string" ? input : input.toString();
        if (url.includes("/api/sessions/history")) {
          return jsonResponse(
            200,
            summaryPage([
              {
                id: withDayId,
                workoutDayId: "ffffffff-bbbb-bbbb-bbbb-000000000001",
                startedAt: "2026-04-12T10:00:00Z",
                endedAt: "2026-04-12T11:00:00Z",
                finished: true,
                setCount: 5,
                mood: null,
                energyLevel: null,
              },
              {
                id: noDayId,
                workoutDayId: null,
                startedAt: "2026-04-13T10:00:00Z",
                endedAt: "2026-04-13T11:00:00Z",
                finished: true,
                setCount: 7,
                mood: null,
                energyLevel: null,
              },
            ])
          );
        }
        if (url.endsWith(`/api/sessions/${withDayId}`)) {
          return jsonResponse(200, {
            id: withDayId,
            workoutDayId: "ffffffff-bbbb-bbbb-bbbb-000000000001",
            startedAt: "2026-04-12T10:00:00Z",
            endedAt: "2026-04-12T11:00:00Z",
            finished: true,
            notes: null,
            mood: null,
            energyLevel: null,
            sets: [],
          });
        }
        if (url.endsWith(`/api/sessions/${noDayId}`)) {
          return jsonResponse(200, {
            id: noDayId,
            workoutDayId: null,
            startedAt: "2026-04-13T10:00:00Z",
            endedAt: "2026-04-13T11:00:00Z",
            finished: true,
            notes: null,
            mood: null,
            energyLevel: null,
            sets: [],
          });
        }
        throw new Error("unexpected fetch: " + url);
      })
    );

    renderClient(<HistoryClient />);
    const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime });

    const dayWithPlan = await screen.findByRole("button", {
      name: "2026-04-12",
    });
    await user.click(dayWithPlan);
    expect(
      await screen.findByRole("button", { name: /repeat workout/i })
    ).toBeInTheDocument();

    const dayWithoutPlan = await screen.findByRole("button", {
      name: "2026-04-13",
    });
    await user.click(dayWithoutPlan);
    await waitFor(() =>
      expect(
        screen.queryByRole("button", { name: /repeat workout/i })
      ).not.toBeInTheDocument()
    );
  });

  it("starts a new session and navigates to /session/{id} when Repeat is clicked", async () => {
    const sessionId = "ffffffff-7777-7777-7777-777777777777";
    const newSessionId = "ffffffff-8888-8888-8888-888888888888";
    let startBody: unknown = null;
    vi.stubGlobal(
      "fetch",
      vi.fn(async (input: string | URL | Request, init?: RequestInit) => {
        const url = typeof input === "string" ? input : input.toString();
        if (url.includes("/api/sessions/history")) {
          return jsonResponse(
            200,
            summaryPage([
              {
                id: sessionId,
                workoutDayId: "ffffffff-bbbb-bbbb-bbbb-000000000001",
                startedAt: "2026-04-14T10:00:00Z",
                endedAt: "2026-04-14T11:00:00Z",
                finished: true,
                setCount: 6,
                mood: null,
                energyLevel: null,
              },
            ])
          );
        }
        if (url.endsWith(`/api/sessions/${sessionId}`)) {
          return jsonResponse(200, {
            id: sessionId,
            workoutDayId: "ffffffff-bbbb-bbbb-bbbb-000000000001",
            startedAt: "2026-04-14T10:00:00Z",
            endedAt: "2026-04-14T11:00:00Z",
            finished: true,
            notes: null,
            mood: null,
            energyLevel: null,
            sets: [],
          });
        }
        if (url.includes("/api/sessions/start")) {
          startBody = init?.body ? JSON.parse(String(init.body)) : null;
          return jsonResponse(201, {
            id: newSessionId,
            workoutDayId: "ffffffff-bbbb-bbbb-bbbb-000000000001",
            startedAt: "2026-04-15T12:00:00Z",
            endedAt: null,
            finished: false,
            notes: null,
            mood: null,
            energyLevel: null,
            sets: [],
          });
        }
        throw new Error("unexpected fetch: " + url);
      })
    );

    renderClient(<HistoryClient />);
    const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime });
    const dayButton = await screen.findByRole("button", {
      name: "2026-04-14",
    });
    await user.click(dayButton);

    const repeatBtn = await screen.findByRole("button", {
      name: /repeat workout/i,
    });
    await user.click(repeatBtn);

    await waitFor(() =>
      expect(pushMock).toHaveBeenCalledWith(`/session/${newSessionId}`)
    );
    expect(startBody).toEqual({ workoutDayId: "ffffffff-bbbb-bbbb-bbbb-000000000001" });
  });

  it("shows the SESSION_ALREADY_ACTIVE toast when Repeat returns 409", async () => {
    const sessionId = "ffffffff-9999-9999-9999-999999999999";
    vi.stubGlobal(
      "fetch",
      vi.fn(async (input: string | URL | Request) => {
        const url = typeof input === "string" ? input : input.toString();
        if (url.includes("/api/sessions/history")) {
          return jsonResponse(
            200,
            summaryPage([
              {
                id: sessionId,
                workoutDayId: "ffffffff-bbbb-bbbb-bbbb-000000000001",
                startedAt: "2026-04-11T10:00:00Z",
                endedAt: "2026-04-11T11:00:00Z",
                finished: true,
                setCount: 3,
                mood: null,
                energyLevel: null,
              },
            ])
          );
        }
        if (url.endsWith(`/api/sessions/${sessionId}`)) {
          return jsonResponse(200, {
            id: sessionId,
            workoutDayId: "ffffffff-bbbb-bbbb-bbbb-000000000001",
            startedAt: "2026-04-11T10:00:00Z",
            endedAt: "2026-04-11T11:00:00Z",
            finished: true,
            notes: null,
            mood: null,
            energyLevel: null,
            sets: [],
          });
        }
        if (url.includes("/api/sessions/start")) {
          return jsonResponse(409, {
            timestamp: "2026-05-09T00:00:00Z",
            status: 409,
            error: "Conflict",
            message: "Already active",
            code: "SESSION_ALREADY_ACTIVE",
            path: "/api/sessions/start",
          });
        }
        throw new Error("unexpected fetch: " + url);
      })
    );

    renderClient(<HistoryClient />);
    const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime });
    const dayButton = await screen.findByRole("button", {
      name: "2026-04-11",
    });
    await user.click(dayButton);

    const repeatBtn = await screen.findByRole("button", {
      name: /repeat workout/i,
    });
    await user.click(repeatBtn);

    await waitFor(() =>
      expect(screen.getByTestId("pr-toast")).toHaveTextContent(
        "You already have an active session. Redirecting to resume."
      )
    );
    expect(pushMock).not.toHaveBeenCalled();
  });
});
