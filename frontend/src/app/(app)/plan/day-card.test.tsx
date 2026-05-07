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

import { DayCard } from "@/app/(app)/plan/day-card";
import { clearTokens } from "@/lib/auth/token-store";
import type { WorkoutDay, WorkoutPlan } from "@/lib/api/schemas";

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
    addDay: "Add day",
    editDay: "Edit",
    deleteDay: "Delete day",
    deleteDayConfirm: "Delete this day?",
    dayDialogCreateTitle: "New day",
    dayDialogEditTitle: "Edit day",
    dayNameLabel: "Day name",
    dayOfWeekLabel: "Weekday",
    focusLabel: "Focus",
    estimatedDurationMinLabel: "Estimated duration (min)",
    focus: {
      PUSH: "Push",
      PULL: "Pull",
      LEGS: "Legs",
      CARDIO: "Cardio",
      CORE: "Core",
      FULL_BODY: "Full body",
      REST: "Rest",
    },
    dayOfWeekTaken: "That weekday is already taken.",
    dialogSave: "Save",
    dialogCancel: "Cancel",
    nameRequired: "Day name is required",
    mutationError: "Operation failed. Try again.",
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

function noContent(): Response {
  return new Response(null, { status: 204 });
}

const planId = "ffffffff-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
const dayId = "ffffffff-bbbb-bbbb-bbbb-bbbbbbbbbbbb";

const plan: WorkoutPlan = {
  id: planId,
  name: "Test Plan",
  active: true,
  createdAt: "2026-04-01T00:00:00Z",
  updatedAt: "2026-04-01T00:00:00Z",
  days: [],
};

const day: WorkoutDay = {
  id: dayId,
  dayOfWeek: 1,
  name: "Monday Push",
  focus: "PUSH",
  estimatedDurationMin: 45,
  exercises: [],
};

describe("DayCard", () => {
  beforeEach(() => {
    clearTokens();
    window.localStorage.clear();
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  it("renders the day's name and weekday + exercise count when day is present", () => {
    renderCard(<DayCard plan={plan} day={day} dayOfWeek={1} />);
    expect(screen.getByText("Monday Push")).toBeInTheDocument();
    expect(screen.getByText(/Monday · 0/)).toBeInTheDocument();
  });

  it("renders an Add day button when day is null and reveals the create dialog on click", async () => {
    renderCard(<DayCard plan={plan} day={null} dayOfWeek={2} />);
    const addBtn = screen.getByTestId("add-day-2");
    expect(addBtn).toBeInTheDocument();
    const user = userEvent.setup();
    await user.click(addBtn);
    expect(await screen.findByTestId("day-edit-dialog")).toBeInTheDocument();
  });

  it("clicking Edit reveals the edit dialog pre-filled with the day data", async () => {
    renderCard(<DayCard plan={plan} day={day} dayOfWeek={1} />);
    const editBtn = screen.getByTestId("edit-day-1");
    const user = userEvent.setup();
    await user.click(editBtn);
    expect(await screen.findByTestId("day-edit-dialog")).toBeInTheDocument();
    const nameInput = screen.getByLabelText("Day name") as HTMLInputElement;
    expect(nameInput.value).toBe("Monday Push");
  });

  it("clicking Delete with confirm=true fires DELETE", async () => {
    vi.spyOn(window, "confirm").mockReturnValue(true);
    let deleteCalledPath: string | null = null;
    vi.stubGlobal(
      "fetch",
      vi.fn(async (input: string | URL | Request, init?: RequestInit) => {
        const url = typeof input === "string" ? input : input.toString();
        if (
          init?.method === "DELETE" &&
          url.includes(`/api/workout-plans/${planId}/days/${dayId}`)
        ) {
          deleteCalledPath = url;
          return noContent();
        }
        throw new Error("unexpected fetch: " + url);
      })
    );

    renderCard(<DayCard plan={plan} day={day} dayOfWeek={1} />);
    const user = userEvent.setup();
    await user.click(screen.getByTestId("delete-day-1"));

    await waitFor(() => expect(deleteCalledPath).not.toBeNull());
    expect(deleteCalledPath).toContain(
      `/api/workout-plans/${planId}/days/${dayId}`
    );
  });
});
