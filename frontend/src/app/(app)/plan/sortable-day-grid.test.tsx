import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import { NextIntlClientProvider } from "next-intl";
import type { ReactElement } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: vi.fn(), replace: vi.fn(), refresh: vi.fn() }),
  useSearchParams: () => new URLSearchParams(),
  usePathname: () => "/plan",
}));

import {
  SortableDayGrid,
  computeDayDragOutcome,
} from "@/app/(app)/plan/sortable-day-grid";
import type { WorkoutDay, WorkoutPlan } from "@/lib/api/schemas";
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
    dayDragHandle: "Drag day",
    dayMoveError: "Could not move day. Try again.",
  },
};

function renderGrid(ui: ReactElement) {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return render(
    <NextIntlClientProvider locale="en" messages={messages}>
      <QueryClientProvider client={client}>{ui}</QueryClientProvider>
    </NextIntlClientProvider>
  );
}

const planId = "ffffffff-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
const dayMonId = "ffffffff-bbbb-bbbb-bbbb-bbbbbbbbbbbb";
const dayWedId = "ffffffff-cccc-cccc-cccc-cccccccccccc";

const mondayDay: WorkoutDay = {
  id: dayMonId,
  dayOfWeek: 1,
  name: "Monday Push",
  focus: "PUSH",
  estimatedDurationMin: 45,
  exercises: [],
};

const wednesdayDay: WorkoutDay = {
  id: dayWedId,
  dayOfWeek: 3,
  name: "Wednesday Pull",
  focus: "PULL",
  estimatedDurationMin: 50,
  exercises: [],
};

function planWith(days: WorkoutDay[]): WorkoutPlan {
  return {
    id: planId,
    name: "Test Plan",
    active: true,
    createdAt: "2026-04-01T00:00:00Z",
    updatedAt: "2026-04-01T00:00:00Z",
    days,
  };
}

describe("SortableDayGrid", () => {
  beforeEach(() => {
    clearTokens();
    window.localStorage.clear();
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  it("renders 7 day-card slots, one per weekday", () => {
    renderGrid(<SortableDayGrid plan={planWith([mondayDay])} />);
    for (let dow = 1; dow <= 7; dow++) {
      expect(screen.getByTestId(`day-card-${dow}`)).toBeInTheDocument();
    }
  });

  it("renders a drag handle for each populated day but not for empty slots", () => {
    renderGrid(<SortableDayGrid plan={planWith([mondayDay, wednesdayDay])} />);
    const monHandle = screen.getByTestId(`day-drag-handle-${dayMonId}`);
    const wedHandle = screen.getByTestId(`day-drag-handle-${dayWedId}`);
    expect(monHandle).toBeInTheDocument();
    expect(monHandle).toHaveAttribute("aria-label", "Drag day");
    expect(wedHandle).toBeInTheDocument();
    // Empty slots (e.g. dayOfWeek=2) should not expose any drag handle.
    expect(screen.queryByTestId("day-drag-handle-empty-2")).toBeNull();
  });

  it("computeDayDragOutcome: noop when active === over", () => {
    const days: ReadonlyArray<WorkoutDay | null> = [
      mondayDay,
      null,
      null,
      null,
      null,
      null,
      null,
    ];
    expect(computeDayDragOutcome(dayMonId, dayMonId, days)).toEqual({
      kind: "noop",
    });
  });

  it("computeDayDragOutcome: move when dropping a real day onto an empty slot", () => {
    const days: ReadonlyArray<WorkoutDay | null> = [
      mondayDay,
      null,
      null,
      null,
      null,
      null,
      null,
    ];
    expect(computeDayDragOutcome(dayMonId, "empty-3", days)).toEqual({
      kind: "move",
      activeDayId: dayMonId,
      targetDow: 3,
    });
  });

  it("computeDayDragOutcome: collision when dropping a real day onto another real day", () => {
    const days: ReadonlyArray<WorkoutDay | null> = [
      mondayDay,
      null,
      wednesdayDay,
      null,
      null,
      null,
      null,
    ];
    expect(computeDayDragOutcome(dayMonId, dayWedId, days)).toEqual({
      kind: "collision",
    });
  });
});
