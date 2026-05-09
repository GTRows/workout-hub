import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

vi.mock("next-intl/server", () => ({
  getTranslations: async () => (key: string) =>
    (
      ({
        offlineTitle: "You are offline",
        offlineDescription:
          "No connection. You can resume once you are back online.",
        offlineRetryLink: "Back to dashboard",
      }) as Record<string, string>
    )[key] ?? key,
}));

import OfflinePage from "./page";

describe("OfflinePage", () => {
  it("renders title, description, and a back-to-dashboard link", async () => {
    const ui = await OfflinePage();
    render(ui);

    expect(screen.getByText("You are offline")).toBeInTheDocument();
    expect(
      screen.getByText(
        "No connection. You can resume once you are back online."
      )
    ).toBeInTheDocument();

    const link = screen.getByRole("link", { name: "Back to dashboard" });
    expect(link).toHaveAttribute("href", "/dashboard");
  });
});
