import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { NextIntlClientProvider } from "next-intl";
import type { ReactElement } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

vi.mock("./plan-client", () => ({
  DayExercises: () => <div data-testid="day-exercises-stub" />,
}));

import { DayExercisesSection } from "@/app/(app)/plan/day-exercises-section";
import { clearTokens } from "@/lib/auth/token-store";
import type { WorkoutDay, WorkoutPlan } from "@/lib/api/schemas";

const messages = {
  plan: {
    dialogSave: "Save",
    dialogCancel: "Cancel",
    mutationError: "Operation failed. Try again.",
    addExercise: "Add exercise",
    editExercise: "Edit",
    removeExercise: "Remove",
    removeExerciseConfirm: "Remove this exercise from the day?",
    exerciseDialogAddTitle: "Add exercise",
    exerciseDialogEditTitle: "Edit exercise",
    exerciseLabel: "Exercise",
    exercisePlaceholder: "Pick an exercise",
    targetSetsLabel: "Sets",
    targetRepsMinLabel: "Min reps",
    targetRepsMaxLabel: "Max reps",
    targetWeightKgLabel: "Weight (kg)",
    restSecondsLabel: "Rest (sec)",
    notesLabel: "Notes",
    exerciseRequired: "Pick an exercise",
    targetSetsRequired: "Sets must be at least 1",
    repsRangeInvalid: "Min reps must be less than or equal to max",
    loadingExercises: "Loading exercises...",
  },
};

function renderSection(ui: ReactElement) {
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
const itemId = "ffffffff-cccc-cccc-cccc-cccccccccccc";
const exerciseUuid = "11111111-1111-1111-1111-111111111111";

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
  exercises: [
    {
      id: itemId,
      exerciseId: exerciseUuid,
      exerciseNameTr: "Bench Press",
      exerciseNameEn: "Bench Press",
      orderIndex: 0,
      targetSets: 4,
      targetRepsMin: 8,
      targetRepsMax: 12,
      targetWeightKg: 60,
      restSeconds: 90,
      notes: null,
    },
  ],
};

function exercisesPagePayload() {
  return {
    content: [
      {
        id: exerciseUuid,
        nameTr: "Bench Press",
        nameEn: "Bench Press",
        category: "push",
        equipment: "bar",
        musclePrimary: "chest",
        muscleSecondary: null,
        descriptionTr: null,
        descriptionEn: null,
        formTipsTr: [],
        formTipsEn: [],
        commonMistakesTr: [],
        commonMistakesEn: [],
        imageUrl: null,
        videoUrl: null,
        difficulty: "intermediate",
      },
    ],
    totalElements: 1,
    totalPages: 1,
    number: 0,
    size: 200,
  };
}

describe("DayExercisesSection", () => {
  beforeEach(() => {
    clearTokens();
    window.localStorage.clear();
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  it("renders the Add exercise button when not adding", () => {
    renderSection(<DayExercisesSection plan={plan} day={day} />);
    expect(screen.getByTestId(`add-exercise-${dayId}`)).toBeInTheDocument();
    expect(screen.getByTestId(`exercise-manage-row-${itemId}`)).toBeInTheDocument();
    expect(screen.queryByTestId("exercise-edit-dialog")).toBeNull();
  });

  it("clicking Add reveals the add dialog", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async (input: string | URL | Request, init?: RequestInit) => {
        const url = typeof input === "string" ? input : input.toString();
        const method = init?.method ?? "GET";
        if (
          method === "GET" &&
          url.includes("/api/exercises") &&
          !url.includes("/search")
        ) {
          return jsonResponse(200, exercisesPagePayload());
        }
        throw new Error("unexpected fetch: " + method + " " + url);
      })
    );

    renderSection(<DayExercisesSection plan={plan} day={day} />);
    const user = userEvent.setup();
    await user.click(screen.getByTestId(`add-exercise-${dayId}`));

    expect(
      await screen.findByTestId("exercise-edit-dialog")
    ).toBeInTheDocument();
  });

  it("submitting add fires POST /exercises and closes the dialog", async () => {
    let postUrl: string | null = null;
    let postBody: unknown = null;
    vi.stubGlobal(
      "fetch",
      vi.fn(async (input: string | URL | Request, init?: RequestInit) => {
        const url = typeof input === "string" ? input : input.toString();
        const method = init?.method ?? "GET";
        if (
          method === "GET" &&
          url.includes("/api/exercises") &&
          !url.includes("/search") &&
          !url.includes("/days/")
        ) {
          return jsonResponse(200, exercisesPagePayload());
        }
        if (
          method === "POST" &&
          url.includes(
            `/api/workout-plans/${planId}/days/${dayId}/exercises`
          )
        ) {
          postUrl = url;
          postBody = init?.body ? JSON.parse(init.body as string) : null;
          return jsonResponse(201, {
            id: "ffffffff-dddd-dddd-dddd-dddddddddddd",
            exerciseId: exerciseUuid,
            exerciseNameTr: "Bench Press",
            exerciseNameEn: "Bench Press",
            orderIndex: 1,
            targetSets: 3,
            targetRepsMin: null,
            targetRepsMax: null,
            targetWeightKg: null,
            restSeconds: null,
            notes: null,
          });
        }
        throw new Error("unexpected fetch: " + method + " " + url);
      })
    );

    renderSection(<DayExercisesSection plan={plan} day={day} />);
    const user = userEvent.setup();
    await user.click(screen.getByTestId(`add-exercise-${dayId}`));

    const select = (await screen.findByTestId(
      "exercise-pick"
    )) as HTMLSelectElement;
    await waitFor(() => expect(select.options.length).toBe(2));
    await user.selectOptions(select, exerciseUuid);

    await user.click(screen.getByRole("button", { name: "Save" }));

    await waitFor(() => expect(postUrl).not.toBeNull());
    expect(postUrl).toContain(
      `/api/workout-plans/${planId}/days/${dayId}/exercises`
    );
    expect(postBody).toMatchObject({
      exerciseId: exerciseUuid,
      targetSets: 3,
    });

    await waitFor(() =>
      expect(screen.queryByTestId("exercise-edit-dialog")).toBeNull()
    );
  });

  it("Edit click reveals the edit dialog for that item", async () => {
    renderSection(<DayExercisesSection plan={plan} day={day} />);
    const user = userEvent.setup();
    await user.click(screen.getByTestId(`edit-exercise-${itemId}`));

    expect(
      await screen.findByTestId("exercise-edit-dialog")
    ).toBeInTheDocument();
    const setsInput = screen.getByLabelText("Sets") as HTMLInputElement;
    expect(setsInput.value).toBe("4");
  });

  it("Remove with confirm=true fires DELETE", async () => {
    vi.spyOn(window, "confirm").mockReturnValue(true);
    let deleteUrl: string | null = null;
    vi.stubGlobal(
      "fetch",
      vi.fn(async (input: string | URL | Request, init?: RequestInit) => {
        const url = typeof input === "string" ? input : input.toString();
        const method = init?.method ?? "GET";
        if (
          method === "DELETE" &&
          url.includes(
            `/api/workout-plans/${planId}/days/${dayId}/exercises/${itemId}`
          )
        ) {
          deleteUrl = url;
          return noContent();
        }
        throw new Error("unexpected fetch: " + method + " " + url);
      })
    );

    renderSection(<DayExercisesSection plan={plan} day={day} />);
    const user = userEvent.setup();
    await user.click(screen.getByTestId(`remove-exercise-${itemId}`));

    await waitFor(() => expect(deleteUrl).not.toBeNull());
    expect(deleteUrl).toContain(
      `/api/workout-plans/${planId}/days/${dayId}/exercises/${itemId}`
    );
  });
});
