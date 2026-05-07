import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { NextIntlClientProvider } from "next-intl";
import type { ReactElement } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { ExerciseEditDialog } from "@/app/(app)/plan/exercise-edit-dialog";
import { clearTokens } from "@/lib/auth/token-store";
import type { WorkoutDayExercise } from "@/lib/api/schemas";

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

function renderDialog(ui: ReactElement) {
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

function exercisesPagePayload() {
  return {
    content: [
      {
        id: "11111111-1111-1111-1111-111111111111",
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
      {
        id: "22222222-2222-2222-2222-222222222222",
        nameTr: "Squat",
        nameEn: "Squat",
        category: "legs",
        equipment: "bar",
        musclePrimary: "quads",
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
      {
        id: "33333333-3333-3333-3333-333333333333",
        nameTr: "Deadlift",
        nameEn: "Deadlift",
        category: "pull",
        equipment: "bar",
        musclePrimary: "back",
        muscleSecondary: null,
        descriptionTr: null,
        descriptionEn: null,
        formTipsTr: [],
        formTipsEn: [],
        commonMistakesTr: [],
        commonMistakesEn: [],
        imageUrl: null,
        videoUrl: null,
        difficulty: "advanced",
      },
    ],
    totalElements: 3,
    totalPages: 1,
    number: 0,
    size: 200,
  };
}

function stubExercisesFetch() {
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
}

const editItem: WorkoutDayExercise = {
  id: "44444444-4444-4444-4444-444444444444",
  exerciseId: "11111111-1111-1111-1111-111111111111",
  exerciseNameTr: "Bench Press",
  exerciseNameEn: "Bench Press",
  orderIndex: 0,
  targetSets: 4,
  targetRepsMin: 8,
  targetRepsMax: 12,
  targetWeightKg: 60,
  restSeconds: 90,
  notes: "go slow",
};

describe("ExerciseEditDialog", () => {
  beforeEach(() => {
    clearTokens();
    window.localStorage.clear();
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  it("renders add-mode with the exercise picker populated", async () => {
    stubExercisesFetch();
    renderDialog(
      <ExerciseEditDialog
        mode="add"
        onSubmit={vi.fn()}
        onCancel={vi.fn()}
      />
    );

    const select = (await screen.findByTestId(
      "exercise-pick"
    )) as HTMLSelectElement;
    await waitFor(() => expect(select.options.length).toBe(4));
    expect(select.options[0].value).toBe("");
    expect(select.options[1].value).toBe(
      "11111111-1111-1111-1111-111111111111"
    );
    expect(select.options[2].value).toBe(
      "22222222-2222-2222-2222-222222222222"
    );
    expect(select.options[3].value).toBe(
      "33333333-3333-3333-3333-333333333333"
    );
  });

  it("renders edit-mode pre-filled and hides the picker", () => {
    stubExercisesFetch();
    renderDialog(
      <ExerciseEditDialog
        mode="edit"
        item={editItem}
        onSubmit={vi.fn()}
        onCancel={vi.fn()}
      />
    );

    const setsInput = screen.getByLabelText("Sets") as HTMLInputElement;
    expect(setsInput.value).toBe("4");
    const repsMin = screen.getByLabelText("Min reps") as HTMLInputElement;
    expect(repsMin.value).toBe("8");
    const repsMax = screen.getByLabelText("Max reps") as HTMLInputElement;
    expect(repsMax.value).toBe("12");
    const weight = screen.getByLabelText("Weight (kg)") as HTMLInputElement;
    expect(weight.value).toBe("60");
    const rest = screen.getByLabelText("Rest (sec)") as HTMLInputElement;
    expect(rest.value).toBe("90");
    const notes = screen.getByLabelText("Notes") as HTMLInputElement;
    expect(notes.value).toBe("go slow");

    expect(screen.queryByTestId("exercise-pick")).toBeNull();
  });

  it("submits add with the normalized payload", async () => {
    stubExercisesFetch();
    const onSubmit = vi.fn();
    renderDialog(
      <ExerciseEditDialog
        mode="add"
        onSubmit={onSubmit}
        onCancel={vi.fn()}
      />
    );

    const select = (await screen.findByTestId(
      "exercise-pick"
    )) as HTMLSelectElement;
    await waitFor(() => expect(select.options.length).toBe(4));

    const user = userEvent.setup();
    await user.selectOptions(select, "11111111-1111-1111-1111-111111111111");

    const setsInput = screen.getByLabelText("Sets");
    await user.clear(setsInput);
    await user.type(setsInput, "3");
    await user.type(screen.getByLabelText("Min reps"), "8");
    await user.type(screen.getByLabelText("Max reps"), "12");
    await user.type(screen.getByLabelText("Weight (kg)"), "60");
    await user.type(screen.getByLabelText("Rest (sec)"), "90");
    await user.type(screen.getByLabelText("Notes"), "  go slow  ");

    await user.click(screen.getByRole("button", { name: "Save" }));

    expect(onSubmit).toHaveBeenCalledTimes(1);
    expect(onSubmit).toHaveBeenCalledWith({
      exerciseId: "11111111-1111-1111-1111-111111111111",
      targetSets: 3,
      targetRepsMin: 8,
      targetRepsMax: 12,
      targetWeightKg: 60,
      restSeconds: 90,
      notes: "go slow",
    });
  });

  it("blocks submit with no exercise picked in add mode", async () => {
    stubExercisesFetch();
    const onSubmit = vi.fn();
    renderDialog(
      <ExerciseEditDialog
        mode="add"
        onSubmit={onSubmit}
        onCancel={vi.fn()}
      />
    );

    await screen.findByTestId("exercise-pick");
    const user = userEvent.setup();
    await user.click(screen.getByRole("button", { name: "Save" }));

    expect(onSubmit).not.toHaveBeenCalled();
    const errors = screen.getAllByText("Pick an exercise");
    expect(errors.some((el) => el.tagName === "P")).toBe(true);
  });

  it("blocks submit with reps min > reps max", async () => {
    stubExercisesFetch();
    const onSubmit = vi.fn();
    renderDialog(
      <ExerciseEditDialog
        mode="add"
        onSubmit={onSubmit}
        onCancel={vi.fn()}
      />
    );

    const select = (await screen.findByTestId(
      "exercise-pick"
    )) as HTMLSelectElement;
    await waitFor(() => expect(select.options.length).toBe(4));

    const user = userEvent.setup();
    await user.selectOptions(select, "11111111-1111-1111-1111-111111111111");

    const setsInput = screen.getByLabelText("Sets");
    await user.clear(setsInput);
    await user.type(setsInput, "3");
    await user.type(screen.getByLabelText("Min reps"), "15");
    await user.type(screen.getByLabelText("Max reps"), "8");

    await user.click(screen.getByRole("button", { name: "Save" }));

    expect(onSubmit).not.toHaveBeenCalled();
    expect(
      screen.getByText("Min reps must be less than or equal to max")
    ).toBeInTheDocument();
  });

  it("calls onCancel when cancel is clicked", async () => {
    stubExercisesFetch();
    const onCancel = vi.fn();
    renderDialog(
      <ExerciseEditDialog
        mode="add"
        onSubmit={vi.fn()}
        onCancel={onCancel}
      />
    );

    const user = userEvent.setup();
    await user.click(screen.getByRole("button", { name: "Cancel" }));
    expect(onCancel).toHaveBeenCalledTimes(1);
  });
});
