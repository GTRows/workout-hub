import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { NextIntlClientProvider } from "next-intl";
import type { ReactElement } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: vi.fn(), replace: vi.fn(), refresh: vi.fn() }),
  useSearchParams: () => new URLSearchParams(),
  usePathname: () => "/plan",
}));

import { PlanClient } from "@/app/(app)/plan/plan-client";
import { clearTokens } from "@/lib/auth/token-store";

const messages = {
  plan: {
    title: "Weekly plan",
    loading: "Loading...",
    noPlan: "No active plan",
    emptyDay: "No exercises",
    moveUp: "Move up",
    moveDown: "Move down",
    weekday: {
      "1": "Monday",
      "2": "Tuesday",
      "3": "Wednesday",
      "4": "Thursday",
      "5": "Friday",
      "6": "Saturday",
      "7": "Sunday",
    },
    setsReps: "{sets} sets x {min}-{max} reps",
    setsOnly: "{sets} sets",
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

function noContent(): Response {
  return new Response(null, { status: 204 });
}

const planId = "ffffffff-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
const dayId = "ffffffff-bbbb-bbbb-bbbb-bbbbbbbbbbbb";
const item1 = {
  id: "ffffffff-1111-1111-1111-111111111111",
  exerciseId: "ffffffff-e111-e111-e111-e11111111111",
  exerciseNameTr: "Sinav",
  exerciseNameEn: "Push-up",
  orderIndex: 1,
  targetSets: 3,
  targetRepsMin: 8,
  targetRepsMax: 12,
};
const item2 = {
  id: "ffffffff-2222-2222-2222-222222222222",
  exerciseId: "ffffffff-e222-e222-e222-e22222222222",
  exerciseNameTr: "Omuz Baski",
  exerciseNameEn: "Shoulder Press",
  orderIndex: 2,
  targetSets: 3,
  targetRepsMin: null,
  targetRepsMax: null,
};

function planWithOneDay() {
  return {
    id: planId,
    name: "Test Plan",
    active: true,
    createdAt: "2026-04-01T00:00:00Z",
    updatedAt: "2026-04-01T00:00:00Z",
    days: [
      {
        id: dayId,
        dayOfWeek: 1,
        name: "Monday Push",
        focus: "push",
        estimatedDurationMin: 45,
        exercises: [item1, item2],
      },
    ],
  };
}

describe("PlanClient", () => {
  beforeEach(() => {
    clearTokens();
    window.localStorage.clear();
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("shows 'No active plan' when /workout-plans/active returns 204", async () => {
    vi.stubGlobal("fetch", vi.fn(async () => noContent()));
    renderClient(<PlanClient />);
    expect(await screen.findByText(/no active plan/i)).toBeInTheDocument();
  });

  it("renders all seven weekdays and surfaces the planned day's name", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => jsonResponse(200, planWithOneDay()))
    );
    renderClient(<PlanClient />);
    expect(await screen.findByRole("heading", { name: "Monday Push" })).toBeInTheDocument();
    // Empty days still render their weekday labels as titles; the title
    // and subtitle both carry the same label for empty days so match by
    // heading role to disambiguate.
    expect(screen.getByRole("heading", { name: "Tuesday" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Sunday" })).toBeInTheDocument();
  });

  it("clicking Move down fires a reorder POST with the new order", async () => {
    let reorderBody: unknown = null;
    vi.stubGlobal(
      "fetch",
      vi.fn(async (input: string | URL | Request, init?: RequestInit) => {
        const url = typeof input === "string" ? input : input.toString();
        if (url.endsWith("/api/workout-plans/active")) {
          return jsonResponse(200, planWithOneDay());
        }
        if (url.includes("/exercises/reorder")) {
          reorderBody = JSON.parse(init!.body as string);
          return jsonResponse(200, {
            ...planWithOneDay().days[0],
            exercises: [item2, item1].map((e, i) => ({
              ...e,
              orderIndex: i + 1,
            })),
          });
        }
        throw new Error("unexpected fetch: " + url);
      })
    );

    renderClient(<PlanClient />);
    const user = userEvent.setup();

    // Expand the Monday card so the exercise rows render.
    await user.click(await screen.findByText("Monday Push"));

    // Move item1 down -> new order [item2, item1]
    const moveDownButtons = await screen.findAllByRole("button", {
      name: /move down/i,
    });
    await user.click(moveDownButtons[0]);

    await waitFor(() => expect(reorderBody).not.toBeNull());
    expect(reorderBody).toEqual({ itemIdsInOrder: [item2.id, item1.id] });
  });
});
