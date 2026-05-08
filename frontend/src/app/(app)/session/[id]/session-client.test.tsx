import "fake-indexeddb/auto";
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
import {
  __resetOfflineDb,
  enqueueSet,
  getOfflineDb,
} from "@/lib/offline/session-set-queue";

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
    queuedOffline: "Offline queued",
    queuedDrained: "{count} queued synced",
    queuedDropped: "Some queued dropped",
    queuedBadge: "{count} pending",
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
    focus: "PUSH",
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

  it("shows the PR celebration toast when the saved set comes back with newPr=true", async () => {
    const user = userEvent.setup();
    const newSet = {
      id: "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
      exerciseId,
      exerciseNameTr: "Sinav",
      exerciseNameEn: "Push-up",
      setNumber: 1,
      repsDone: 10,
      weightKg: 50,
      rpe: null,
      completed: true,
      notes: null,
      newPr: true,
    };
    vi.stubGlobal(
      "fetch",
      routeFetch({
        [`/api/sessions/${sessionId}`]: () => jsonResponse(200, baseSession()),
        [`/api/workout-days/${dayId}`]: () => jsonResponse(200, dayWithOneExercise()),
        [`/api/exercises/${exerciseId}/last-performance`]: () => noContent(),
        [`/api/sessions/${sessionId}/sets`]: () => jsonResponse(201, newSet),
      })
    );
    renderClient(<SessionClient sessionId={sessionId} />);
    await screen.findByText("Sinav");
    await user.type(screen.getByLabelText("Reps"), "10");
    await user.type(screen.getByLabelText("Weight"), "50");
    await user.click(screen.getByRole("button", { name: "Done" }));
    expect(await screen.findByTestId("pr-toast")).toBeInTheDocument();
  });

  it("does not show the PR toast for a regular non-PR set", async () => {
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
    vi.stubGlobal(
      "fetch",
      routeFetch({
        [`/api/sessions/${sessionId}`]: () => jsonResponse(200, baseSession()),
        [`/api/workout-days/${dayId}`]: () => jsonResponse(200, dayWithOneExercise()),
        [`/api/exercises/${exerciseId}/last-performance`]: () => noContent(),
        [`/api/sessions/${sessionId}/sets`]: () => jsonResponse(201, newSet),
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
    expect(screen.queryByTestId("pr-toast")).not.toBeInTheDocument();
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

  it("includes a clientSetId UUID in the addSet request body", async () => {
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
    const body = JSON.parse(posted!.body as string) as { clientSetId?: unknown };
    expect(typeof body.clientSetId).toBe("string");
    expect(body.clientSetId).toEqual(
      expect.stringMatching(
        /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i
      )
    );
  });
});

describe("SessionClient offline drain wiring", () => {
  beforeEach(async () => {
    clearTokens();
    window.localStorage.clear();
    pushMock.mockReset();
    __resetOfflineDb();
    await getOfflineDb().queuedSets.clear();
    Object.defineProperty(navigator, "onLine", {
      value: true,
      configurable: true,
    });
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
    Object.defineProperty(navigator, "onLine", {
      value: true,
      configurable: true,
    });
  });

  it("enqueues the submit when navigator.onLine is false and shows the queued toast", async () => {
    Object.defineProperty(navigator, "onLine", {
      value: false,
      configurable: true,
    });

    vi.stubGlobal(
      "fetch",
      routeFetch({
        [`/api/sessions/${sessionId}`]: () => jsonResponse(200, baseSession()),
        [`/api/workout-days/${dayId}`]: () =>
          jsonResponse(200, dayWithOneExercise()),
        [`/api/exercises/${exerciseId}/last-performance`]: () => noContent(),
      })
    );

    renderClient(<SessionClient sessionId={sessionId} />);
    await screen.findByText("Sinav");

    const user = userEvent.setup();
    await user.type(screen.getByLabelText("Reps"), "10");
    await user.click(screen.getByRole("button", { name: "Done" }));

    await waitFor(() =>
      expect(screen.getByTestId("pr-toast")).toHaveTextContent("Offline queued")
    );

    expect(screen.getByText("1 pending")).toBeInTheDocument();

    const items = await getOfflineDb()
      .queuedSets.where("sessionId")
      .equals(sessionId)
      .toArray();
    expect(items).toHaveLength(1);
    expect(items[0]?.payload.repsDone).toBe(10);
  });

  it("drains the queue on mount when there are pre-existing items", async () => {
    await enqueueSet(sessionId, {
      exerciseId,
      setNumber: 1,
      repsDone: 8,
      weightKg: 20,
      completed: true,
      clientSetId: "11111111-1111-4111-8111-111111111111",
    });
    await enqueueSet(sessionId, {
      exerciseId,
      setNumber: 2,
      repsDone: 9,
      weightKg: 22.5,
      completed: true,
      clientSetId: "22222222-2222-4222-8222-222222222222",
    });

    let postCount = 0;
    const newSet = (n: number) => ({
      id: `aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeee${n}`,
      exerciseId,
      exerciseNameTr: "Sinav",
      exerciseNameEn: "Push-up",
      setNumber: n,
      repsDone: n + 7,
      weightKg: 20 + (n - 1) * 2.5,
      rpe: null,
      completed: true,
      notes: null,
    });

    vi.stubGlobal(
      "fetch",
      routeFetch({
        [`/api/sessions/${sessionId}`]: () => jsonResponse(200, baseSession()),
        [`/api/workout-days/${dayId}`]: () =>
          jsonResponse(200, dayWithOneExercise()),
        [`/api/exercises/${exerciseId}/last-performance`]: () => noContent(),
        [`/api/sessions/${sessionId}/sets`]: () => {
          postCount++;
          return jsonResponse(201, newSet(postCount));
        },
      })
    );

    renderClient(<SessionClient sessionId={sessionId} />);
    await screen.findByText("Sinav");

    await waitFor(() => expect(postCount).toBe(2));

    await waitFor(async () => {
      const items = await getOfflineDb()
        .queuedSets.where("sessionId")
        .equals(sessionId)
        .toArray();
      expect(items).toHaveLength(0);
    });

    await waitFor(() =>
      expect(screen.getByTestId("pr-toast")).toHaveTextContent("2 queued synced")
    );
  });

  it("drains the queue when the window online event fires", async () => {
    Object.defineProperty(navigator, "onLine", {
      value: false,
      configurable: true,
    });

    let postCount = 0;
    vi.stubGlobal(
      "fetch",
      routeFetch({
        [`/api/sessions/${sessionId}`]: () => jsonResponse(200, baseSession()),
        [`/api/workout-days/${dayId}`]: () =>
          jsonResponse(200, dayWithOneExercise()),
        [`/api/exercises/${exerciseId}/last-performance`]: () => noContent(),
        [`/api/sessions/${sessionId}/sets`]: () => {
          postCount++;
          return jsonResponse(201, {
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
          });
        },
      })
    );

    renderClient(<SessionClient sessionId={sessionId} />);
    await screen.findByText("Sinav");

    const user = userEvent.setup();
    await user.type(screen.getByLabelText("Reps"), "10");
    await user.type(screen.getByLabelText("Weight"), "20");
    await user.click(screen.getByRole("button", { name: "Done" }));

    await waitFor(() =>
      expect(screen.getByText("1 pending")).toBeInTheDocument()
    );
    expect(postCount).toBe(0);

    Object.defineProperty(navigator, "onLine", {
      value: true,
      configurable: true,
    });
    window.dispatchEvent(new Event("online"));

    await waitFor(() => expect(postCount).toBe(1));
    await waitFor(() =>
      expect(screen.queryByText("1 pending")).not.toBeInTheDocument()
    );
  });

  it("drops permanent-reject items mid-drain and shows the dropped toast", async () => {
    await enqueueSet(sessionId, {
      exerciseId,
      setNumber: 1,
      repsDone: 8,
      completed: true,
      clientSetId: "33333333-3333-4333-8333-333333333333",
    });
    await enqueueSet(sessionId, {
      exerciseId,
      setNumber: 2,
      repsDone: 9,
      completed: true,
      clientSetId: "44444444-4444-4444-8444-444444444444",
    });

    let postCount = 0;
    vi.stubGlobal(
      "fetch",
      routeFetch({
        [`/api/sessions/${sessionId}`]: () => jsonResponse(200, baseSession()),
        [`/api/workout-days/${dayId}`]: () =>
          jsonResponse(200, dayWithOneExercise()),
        [`/api/exercises/${exerciseId}/last-performance`]: () => noContent(),
        [`/api/sessions/${sessionId}/sets`]: () => {
          postCount++;
          if (postCount === 1) {
            return jsonResponse(409, {
              timestamp: "2026-05-08T00:00:00Z",
              status: 409,
              error: "Conflict",
              message: "Session already finished",
              code: "SESSION_FINISHED",
              path: `/api/sessions/${sessionId}/sets`,
            });
          }
          return jsonResponse(201, {
            id: "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
            exerciseId,
            exerciseNameTr: "Sinav",
            exerciseNameEn: "Push-up",
            setNumber: 2,
            repsDone: 9,
            weightKg: null,
            rpe: null,
            completed: true,
            notes: null,
          });
        },
      })
    );

    renderClient(<SessionClient sessionId={sessionId} />);
    await screen.findByText("Sinav");

    await waitFor(() => expect(postCount).toBe(2));
    await waitFor(() =>
      expect(screen.getByTestId("pr-toast")).toHaveTextContent(
        "Some queued dropped"
      )
    );

    const items = await getOfflineDb()
      .queuedSets.where("sessionId")
      .equals(sessionId)
      .toArray();
    expect(items).toHaveLength(0);
  });

  it("leaves the queue intact when the drain hits a transient network error", async () => {
    await enqueueSet(sessionId, {
      exerciseId,
      setNumber: 1,
      repsDone: 8,
      completed: true,
      clientSetId: "55555555-5555-4555-8555-555555555555",
    });
    await enqueueSet(sessionId, {
      exerciseId,
      setNumber: 2,
      repsDone: 9,
      completed: true,
      clientSetId: "66666666-6666-4666-8666-666666666666",
    });

    let postCount = 0;
    vi.stubGlobal(
      "fetch",
      routeFetch({
        [`/api/sessions/${sessionId}`]: () => jsonResponse(200, baseSession()),
        [`/api/workout-days/${dayId}`]: () =>
          jsonResponse(200, dayWithOneExercise()),
        [`/api/exercises/${exerciseId}/last-performance`]: () => noContent(),
        [`/api/sessions/${sessionId}/sets`]: () => {
          postCount++;
          throw new TypeError("network down");
        },
      })
    );

    renderClient(<SessionClient sessionId={sessionId} />);
    await screen.findByText("Sinav");

    await waitFor(() => expect(postCount).toBeGreaterThanOrEqual(1));

    const items = await getOfflineDb()
      .queuedSets.where("sessionId")
      .equals(sessionId)
      .toArray();
    expect(items).toHaveLength(2);

    expect(screen.queryByTestId("pr-toast")).not.toBeInTheDocument();
  });
});

describe("SessionClient typed error UX", () => {
  beforeEach(async () => {
    clearTokens();
    window.localStorage.clear();
    pushMock.mockReset();
    __resetOfflineDb();
    await getOfflineDb().queuedSets.clear();
    Object.defineProperty(navigator, "onLine", {
      value: true,
      configurable: true,
    });
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  it("shows the SESSION_FINISHED toast and routes to /dashboard when addSet returns 409 + code SESSION_FINISHED on the live submit", async () => {
    const user = userEvent.setup();
    vi.stubGlobal(
      "fetch",
      routeFetch({
        [`/api/sessions/${sessionId}`]: () => jsonResponse(200, baseSession()),
        [`/api/workout-days/${dayId}`]: () =>
          jsonResponse(200, dayWithOneExercise()),
        [`/api/exercises/${exerciseId}/last-performance`]: () => noContent(),
        [`/api/sessions/${sessionId}/sets`]: () =>
          jsonResponse(409, {
            timestamp: "2026-05-09T00:00:00Z",
            status: 409,
            error: "Conflict",
            message: "Session already finished",
            code: "SESSION_FINISHED",
            path: `/api/sessions/${sessionId}/sets`,
          }),
      })
    );

    renderClient(<SessionClient sessionId={sessionId} />);
    await screen.findByText("Sinav");

    await user.type(screen.getByLabelText("Reps"), "10");
    await user.click(screen.getByRole("button", { name: "Done" }));

    await waitFor(() =>
      expect(screen.getByTestId("pr-toast")).toHaveTextContent(
        "Session was finished while you were submitting. Returning to dashboard."
      )
    );
    await waitFor(() =>
      expect(pushMock).toHaveBeenCalledWith("/dashboard")
    );
  });

  it("shows the SET_NUMBER_DUPLICATE toast and refetches the session when addSet returns 409 + code SET_NUMBER_DUPLICATE", async () => {
    const user = userEvent.setup();
    let sessionFetchCount = 0;
    vi.stubGlobal(
      "fetch",
      routeFetch({
        [`/api/sessions/${sessionId}`]: () => {
          sessionFetchCount++;
          return jsonResponse(200, baseSession());
        },
        [`/api/workout-days/${dayId}`]: () =>
          jsonResponse(200, dayWithOneExercise()),
        [`/api/exercises/${exerciseId}/last-performance`]: () => noContent(),
        [`/api/sessions/${sessionId}/sets`]: () =>
          jsonResponse(409, {
            timestamp: "2026-05-09T00:00:00Z",
            status: 409,
            error: "Conflict",
            message: "Set number duplicate",
            code: "SET_NUMBER_DUPLICATE",
            path: `/api/sessions/${sessionId}/sets`,
          }),
      })
    );

    renderClient(<SessionClient sessionId={sessionId} />);
    await screen.findByText("Sinav");
    const initialFetchCount = sessionFetchCount;

    await user.type(screen.getByLabelText("Reps"), "10");
    await user.click(screen.getByRole("button", { name: "Done" }));

    await waitFor(() =>
      expect(screen.getByTestId("pr-toast")).toHaveTextContent(
        "Set number conflict, list refreshed. Please try again."
      )
    );
    await waitFor(() =>
      expect(sessionFetchCount).toBeGreaterThan(initialFetchCount)
    );
    expect(pushMock).not.toHaveBeenCalled();
  });

  it("shows the SESSION_ALREADY_FINISHED toast and routes to /dashboard when finishSession returns 409 + code SESSION_ALREADY_FINISHED", async () => {
    const user = userEvent.setup();
    const confirmSpy = vi.spyOn(window, "confirm").mockReturnValue(true);

    vi.stubGlobal(
      "fetch",
      routeFetch({
        [`/api/sessions/${sessionId}`]: () => jsonResponse(200, baseSession()),
        [`/api/workout-days/${dayId}`]: () =>
          jsonResponse(200, dayWithOneExercise()),
        [`/api/exercises/${exerciseId}/last-performance`]: () => noContent(),
        [`/api/sessions/${sessionId}/finish`]: () =>
          jsonResponse(409, {
            timestamp: "2026-05-09T00:00:00Z",
            status: 409,
            error: "Conflict",
            message: "Session already finished",
            code: "SESSION_ALREADY_FINISHED",
            path: `/api/sessions/${sessionId}/finish`,
          }),
      })
    );

    renderClient(<SessionClient sessionId={sessionId} />);
    await screen.findByText("Sinav");

    await user.click(screen.getByRole("button", { name: /finish workout/i }));
    expect(confirmSpy).toHaveBeenCalled();

    await waitFor(() =>
      expect(screen.getByTestId("pr-toast")).toHaveTextContent(
        "This session is already finished. Returning to dashboard."
      )
    );
    await waitFor(() =>
      expect(pushMock).toHaveBeenCalledWith("/dashboard")
    );
  });
});
