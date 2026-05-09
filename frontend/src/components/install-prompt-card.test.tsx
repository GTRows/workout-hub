import { act, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { NextIntlClientProvider } from "next-intl";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { InstallPromptCard } from "./install-prompt-card";

const messages = {
  pwa: {
    installTitle: "Install WorkoutHub",
    installDescription:
      "Add to your home screen for faster, offline-capable access.",
    installButton: "Install",
    installDismiss: "Not now",
    installInstalled: "Installed",
    offlineTitle: "You are offline",
    offlineDescription:
      "No connection. You can resume once you are back online.",
    offlineRetryLink: "Back to dashboard",
  },
};

function renderCard() {
  return render(
    <NextIntlClientProvider locale="en" messages={messages}>
      <InstallPromptCard />
    </NextIntlClientProvider>
  );
}

class FakeBeforeInstallPromptEvent extends Event {
  platforms = ["web"];
  userChoice: Promise<{
    outcome: "accepted" | "dismissed";
    platform: string;
  }>;
  prompt = vi.fn(() => Promise.resolve());
  constructor(outcome: "accepted" | "dismissed" = "accepted") {
    super("beforeinstallprompt");
    this.userChoice = Promise.resolve({ outcome, platform: "web" });
  }
}

describe("InstallPromptCard", () => {
  beforeEach(() => {
    window.localStorage.clear();
    Object.defineProperty(window, "matchMedia", {
      configurable: true,
      value: () => ({
        matches: false,
        addEventListener: () => {},
        removeEventListener: () => {},
      }),
    });
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("renders nothing until beforeinstallprompt fires", () => {
    const { container } = renderCard();
    expect(container).toBeEmptyDOMElement();
  });

  it("shows the card after beforeinstallprompt fires", async () => {
    renderCard();
    const evt = new FakeBeforeInstallPromptEvent("accepted");
    await act(async () => {
      window.dispatchEvent(evt);
    });
    expect(screen.getByText("Install WorkoutHub")).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: "Install" })
    ).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: "Not now" })
    ).toBeInTheDocument();
  });

  it("calls event.prompt() and hides on Install click", async () => {
    renderCard();
    const evt = new FakeBeforeInstallPromptEvent("accepted");
    await act(async () => {
      window.dispatchEvent(evt);
    });

    const user = userEvent.setup();
    await user.click(screen.getByRole("button", { name: "Install" }));

    expect(evt.prompt).toHaveBeenCalledTimes(1);
    expect(screen.queryByText("Install WorkoutHub")).not.toBeInTheDocument();
  });

  it("sets dismissed flag on Not now and hides", async () => {
    renderCard();
    const evt = new FakeBeforeInstallPromptEvent("dismissed");
    await act(async () => {
      window.dispatchEvent(evt);
    });

    const user = userEvent.setup();
    await user.click(screen.getByRole("button", { name: "Not now" }));

    expect(window.localStorage.getItem("wh.installPromptDismissed")).toBe("1");
    expect(screen.queryByText("Install WorkoutHub")).not.toBeInTheDocument();
  });
});
