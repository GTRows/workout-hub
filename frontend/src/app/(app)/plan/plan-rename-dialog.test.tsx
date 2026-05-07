import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { NextIntlClientProvider } from "next-intl";
import type { ReactElement } from "react";
import { describe, expect, it, vi } from "vitest";

import { PlanRenameDialog } from "@/app/(app)/plan/plan-rename-dialog";

const messages = {
  plan: {
    renameDialogTitle: "Rename plan",
    dialogSave: "Save",
    dialogCancel: "Cancel",
    nameRequired: "Plan name is required",
  },
};

function renderDialog(ui: ReactElement) {
  return render(
    <NextIntlClientProvider locale="en" messages={messages}>
      {ui}
    </NextIntlClientProvider>
  );
}

describe("PlanRenameDialog", () => {
  it("renders pre-filled with the initial name", () => {
    renderDialog(
      <PlanRenameDialog
        initialName="Push Pull Legs"
        onSubmit={vi.fn()}
        onCancel={vi.fn()}
      />
    );
    const input = screen.getByLabelText("Rename plan") as HTMLInputElement;
    expect(input.value).toBe("Push Pull Legs");
  });

  it("calls onSubmit with the trimmed new name", async () => {
    const onSubmit = vi.fn();
    renderDialog(
      <PlanRenameDialog
        initialName="Old"
        onSubmit={onSubmit}
        onCancel={vi.fn()}
      />
    );
    const user = userEvent.setup();
    const input = screen.getByLabelText("Rename plan");
    await user.clear(input);
    await user.type(input, "  PPL v2  ");
    await user.click(screen.getByRole("button", { name: "Save" }));
    expect(onSubmit).toHaveBeenCalledTimes(1);
    expect(onSubmit).toHaveBeenCalledWith("PPL v2");
  });

  it("calls onCancel when the cancel button is clicked", async () => {
    const onCancel = vi.fn();
    renderDialog(
      <PlanRenameDialog
        initialName="Old"
        onSubmit={vi.fn()}
        onCancel={onCancel}
      />
    );
    const user = userEvent.setup();
    await user.click(screen.getByRole("button", { name: "Cancel" }));
    expect(onCancel).toHaveBeenCalledTimes(1);
  });
});
