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
  usePathname: () => "/session/abc",
}));

import { SessionClient } from "@/app/(app)/session/[id]/session-client";
import { clearTokens } from "@/lib/auth/token-store";

const messages = {
  session: {
    loading: "Loading...",
    notFound: "Session not found",
    adHocTitle: "Ad-hoc",
    startedAt: "Started: {time}",
    target: "Target: {sets} sets, {min}-{max} reps",
    targetSimple: "Target: {sets} sets",
    lastPerformance: "Last",
    lastPerformanceNone: "First time",
    reps: "Reps",
    weightKg: "Weight",
    complete: "Done",
    setLine: "{n}. {reps} x {weight}kg",
    setLineNoWeight: "{n}. {reps} reps",
    restTimer: "Rest: {seconds}s",
    finishButton: "Finish workout",
    finishConfirm: "Finish?",
    finished: "Finished",
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

function noContent() {
  return new Response(null, { status: 204 });
}

const sessionId = "ffffffff-1111-1111-1111-111111111111";
const dayId = "ffffffff-2222-2222-2222-222222222222";
const planItemId = "ffffffff-3333-3333-3333-333333333333";
const exerciseId = "ffffffff-4444-4444-4444-444444444444";

function baseSession(sets: unknown[] = []) {
  return {
    id: sessionId,
    workoutDayId: dayId,
    startedAt: "2026-04-23T10:00:00Z",
    endedAt: null,
    finished: false,
    sets,
  };
}

function dayWithOneExercise() {
  return {
    id: dayId,
    dayOfWeek: 3,
    name: "Push Day",
    focus: "push",
    estimatedDurationMin: 45,
    exercises: [
      {
        id: planItemId,
        exerciseId,
        exerciseNameTr: "Sinav",
        exerciseNameEn: "Push-up",
        orderIndex: 1,
        targetSets: 3,
        targetRepsMin: 8,
        targetRepsMax: 12,
      },
    ],
  };
}

function routeFetch(routes: Record<string, (init: RequestInit | undefined) => Response>) {
  return vi.fn(async (input: string | URL | Request, init?: RequestInit) => {
    const url = typeof input === "string" ? input : input.toString();
    for (const [pattern, factory] of Object.entries(routes)) {
      if (url.endsWith(pattern)) return factory(init);
    }
    throw new Error("unexpected fetch: " + url);
  });
}

describe("SessionClient", () => {
  beforeEach(() => {
    clearTokens();
    window.localStorage.clear();
    pushMock.mockReset();
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  it("renders planned exercise card from the session's workout day", async () => {
    const fetchMock = routeFetch({
      [`/api/sessions/${sessionId}`]: () => jsonResponse(200, baseSession()),
      [`/api/workout-days/${dayId}`]: () => jsonResponse(200, dayWithOneExercise()),
      [`/api/exercises/${exerciseId}/last-performance`]: () => noContent(),
    });
    vi.stubGlobal("fetch", fetchMock);

    renderClient(<SessionClient sessionId={sessionId} />);
    expect(await screen.findByText("Push Day")).toBeInTheDocument();
    // The TR name wins the ?? chain since the exercise has both locales seeded.
    expect(await screen.findByText("Sinav")).toBeInTheDocument();
    expect(screen.getByText(/target: 3 sets, 8-12 reps/i)).toBeInTheDocument();
    expect(await screen.findByText("First time")).toBeInTheDocument();
  });

  it("posts a new set and appends it to the list", async () => {
    const user = userEvent.setup();
    const newSet = {
      id: "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
      exerciseId,
      exerciseNameTr: "Sinav",
      exerciseNameEn: "Push-up",
      setNumber: 1,
      repsDone: 10,
      weightKg: 20,
      rpe: null,
      completed: true,
      notes: null,
    };

    let posted: RequestInit | undefined;
    vi.stubGlobal(
      "fetch",
      routeFetch({
        [`/api/sessions/${sessionId}`]: () => jsonResponse(200, baseSession()),
        [`/api/workout-days/${dayId}`]: () => jsonResponse(200, dayWithOneExercise()),
        [`/api/exercises/${exerciseId}/last-performance`]: () => noContent(),
        [`/api/sessions/${sessionId}/sets`]: (init) => {
          posted = init;
          return jsonResponse(201, newSet);
        },
      })
    );

    renderClient(<SessionClient sessionId={sessionId} />);
    await screen.findByText("Sinav");

    await user.type(screen.getByLabelText("Reps"), "10");
    await user.type(screen.getByLabelText("Weight"), "20");
    await user.click(screen.getByRole("button", { name: "Done" }));

    await waitFor(() =>
      expect(screen.getByText(/^1\. 10 x 20kg$/)).toBeInTheDocument()
    );
    expect(posted).toBeDefined();
    const payload = JSON.parse(posted!.body as string);
    expect(payload).toMatchObject({
      exerciseId,
      setNumber: 1,
      repsDone: 10,
      weightKg: 20,
      completed: true,
    });
  });

  it("finishes the workout on confirm and navigates to /dashboard", async () => {
    const user = userEvent.setup();
    const confirmSpy = vi.spyOn(window, "confirm").mockReturnValue(true);

    vi.stubGlobal(
      "fetch",
      routeFetch({
        [`/api/sessions/${sessionId}`]: () => jsonResponse(200, baseSession()),
        [`/api/workout-days/${dayId}`]: () => jsonResponse(200, dayWithOneExercise()),
        [`/api/exercises/${exerciseId}/last-performance`]: () => noContent(),
        [`/api/sessions/${sessionId}/finish`]: () =>
          jsonResponse(200, {
            ...baseSession(),
            endedAt: "2026-04-23T11:00:00Z",
            finished: true,
          }),
      })
    );

    renderClient(<SessionClient sessionId={sessionId} />);
    await screen.findByText("Sinav");

    await user.click(screen.getByRole("button", { name: /finish workout/i }));
    expect(confirmSpy).toHaveBeenCalled();
    await waitFor(() => expect(pushMock).toHaveBeenCalledWith("/dashboard"));
  });
});
