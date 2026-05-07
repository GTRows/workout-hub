import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { NextIntlClientProvider } from "next-intl";
import type { ReactElement } from "react";
import { describe, expect, it, vi } from "vitest";

import { DayEditDialog } from "@/app/(app)/plan/day-edit-dialog";
import type { WorkoutDay } from "@/lib/api/schemas";

const messages = {
  plan: {
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
    weekday: {
      "1": "Monday",
      "2": "Tuesday",
      "3": "Wednesday",
      "4": "Thursday",
      "5": "Friday",
      "6": "Saturday",
      "7": "Sunday",
    },
    dialogSave: "Save",
    dialogCancel: "Cancel",
    nameRequired: "Day name is required",
    mutationError: "Operation failed. Try again.",
  },
};

function renderDialog(ui: ReactElement) {
  return render(
    <NextIntlClientProvider locale="en" messages={messages}>
      {ui}
    </NextIntlClientProvider>
  );
}

const editDay: WorkoutDay = {
  id: "ffffffff-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
  dayOfWeek: 2,
  name: "Pull Day",
  focus: "PULL",
  estimatedDurationMin: 45,
  exercises: [],
};

describe("DayEditDialog", () => {
  it("renders create-mode with empty name and the default dayOfWeek pre-selected", () => {
    renderDialog(
      <DayEditDialog
        mode="create"
        defaultDayOfWeek={3}
        onSubmit={vi.fn()}
        onCancel={vi.fn()}
      />
    );
    const nameInput = screen.getByLabelText("Day name") as HTMLInputElement;
    expect(nameInput.value).toBe("");

    const dowSelect = screen.getByLabelText("Weekday") as HTMLSelectElement;
    expect(dowSelect.value).toBe("3");

    const focusSelect = screen.getByLabelText("Focus") as HTMLSelectElement;
    expect(focusSelect.options.length).toBe(7);
    expect(focusSelect.value).toBe("PUSH");
  });

  it("renders edit-mode pre-filled with the existing day data", () => {
    renderDialog(
      <DayEditDialog
        mode="edit"
        day={editDay}
        onSubmit={vi.fn()}
        onCancel={vi.fn()}
      />
    );
    const nameInput = screen.getByLabelText("Day name") as HTMLInputElement;
    expect(nameInput.value).toBe("Pull Day");

    const dowSelect = screen.getByLabelText("Weekday") as HTMLSelectElement;
    expect(dowSelect.value).toBe("2");

    const focusSelect = screen.getByLabelText("Focus") as HTMLSelectElement;
    expect(focusSelect.value).toBe("PULL");

    const durationInput = screen.getByLabelText(
      "Estimated duration (min)"
    ) as HTMLInputElement;
    expect(durationInput.value).toBe("45");
  });

  it("submits create with the normalized payload", async () => {
    const onSubmit = vi.fn();
    renderDialog(
      <DayEditDialog
        mode="create"
        defaultDayOfWeek={1}
        onSubmit={onSubmit}
        onCancel={vi.fn()}
      />
    );
    const user = userEvent.setup();
    const nameInput = screen.getByLabelText("Day name");
    await user.type(nameInput, "  Push Day  ");
    const durationInput = screen.getByLabelText("Estimated duration (min)");
    await user.type(durationInput, "60");
    await user.click(screen.getByRole("button", { name: "Save" }));

    expect(onSubmit).toHaveBeenCalledTimes(1);
    expect(onSubmit).toHaveBeenCalledWith({
      dayOfWeek: 1,
      name: "Push Day",
      focus: "PUSH",
      estimatedDurationMin: 60,
    });
  });

  it("blocks submit with empty name and shows nameRequired", async () => {
    const onSubmit = vi.fn();
    renderDialog(
      <DayEditDialog
        mode="create"
        defaultDayOfWeek={1}
        onSubmit={onSubmit}
        onCancel={vi.fn()}
      />
    );
    const user = userEvent.setup();
    await user.click(screen.getByRole("button", { name: "Save" }));
    expect(onSubmit).not.toHaveBeenCalled();
    expect(screen.getByText("Day name is required")).toBeInTheDocument();
  });

  it("calls onCancel when cancel is clicked", async () => {
    const onCancel = vi.fn();
    renderDialog(
      <DayEditDialog
        mode="create"
        defaultDayOfWeek={1}
        onSubmit={vi.fn()}
        onCancel={onCancel}
      />
    );
    const user = userEvent.setup();
    await user.click(screen.getByRole("button", { name: "Cancel" }));
    expect(onCancel).toHaveBeenCalledTimes(1);
  });
});
