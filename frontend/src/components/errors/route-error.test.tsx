import { fireEvent, render, screen } from "@testing-library/react";
import { NextIntlClientProvider } from "next-intl";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { RouteError } from "./route-error";

const messages = {
  common: {
    loading: "Loading",
    save: "Save",
    cancel: "Cancel",
    delete: "Delete",
    edit: "Edit",
    error: "Something went wrong",
    errorHint: "Try again, or return to the dashboard.",
    tryAgain: "Try again",
    goHome: "Dashboard",
  },
};

function renderWithIntl(error: Error & { digest?: string }, reset: () => void) {
  return render(
    <NextIntlClientProvider locale="en" messages={messages}>
      <RouteError error={error} reset={reset} />
    </NextIntlClientProvider>
  );
}

describe("RouteError", () => {
  let consoleSpy: ReturnType<typeof vi.spyOn>;

  beforeEach(() => {
    consoleSpy = vi.spyOn(console, "error").mockImplementation(() => {});
  });

  afterEach(() => {
    consoleSpy.mockRestore();
  });

  it("renders an element with role=alert", () => {
    const error = Object.assign(new Error("boom"), { digest: "d1" });
    renderWithIntl(error, () => {});

    expect(screen.getByRole("alert")).toBeTruthy();
  });

  it("renders heading and hint copy from translations", () => {
    const error = new Error("boom");
    renderWithIntl(error, () => {});

    expect(screen.getByText("Something went wrong")).toBeTruthy();
    expect(
      screen.getByText("Try again, or return to the dashboard.")
    ).toBeTruthy();
  });

  it("calls reset exactly once when Try again is clicked", () => {
    const error = new Error("boom");
    const reset = vi.fn();
    renderWithIntl(error, reset);

    const button = screen.getByRole("button", { name: /try again/i });
    fireEvent.click(button);

    expect(reset).toHaveBeenCalledTimes(1);
  });

  it("renders a dashboard link with href=/dashboard", () => {
    const error = new Error("boom");
    renderWithIntl(error, () => {});

    const link = screen.getByRole("link", { name: /dashboard/i });
    expect(link.getAttribute("href")).toBe("/dashboard");
  });

  it("logs error message and digest exactly once on mount", () => {
    const error = Object.assign(new Error("boom"), { digest: "abc123" });
    renderWithIntl(error, () => {});

    expect(consoleSpy).toHaveBeenCalledTimes(1);
    expect(consoleSpy).toHaveBeenCalledWith("Route error", {
      message: "boom",
      digest: "abc123",
    });
  });

  it("does not re-log when re-rendered with the same error reference", () => {
    const error = new Error("boom");
    const reset = () => {};
    const { rerender } = render(
      <NextIntlClientProvider locale="en" messages={messages}>
        <RouteError error={error} reset={reset} />
      </NextIntlClientProvider>
    );

    expect(consoleSpy).toHaveBeenCalledTimes(1);

    rerender(
      <NextIntlClientProvider locale="en" messages={messages}>
        <RouteError error={error} reset={reset} />
      </NextIntlClientProvider>
    );

    expect(consoleSpy).toHaveBeenCalledTimes(1);
  });

  it("re-logs when re-rendered with a new error reference", () => {
    const first = new Error("first");
    const reset = () => {};
    const { rerender } = render(
      <NextIntlClientProvider locale="en" messages={messages}>
        <RouteError error={first} reset={reset} />
      </NextIntlClientProvider>
    );

    expect(consoleSpy).toHaveBeenCalledTimes(1);

    const second = Object.assign(new Error("second"), { digest: "d2" });
    rerender(
      <NextIntlClientProvider locale="en" messages={messages}>
        <RouteError error={second} reset={reset} />
      </NextIntlClientProvider>
    );

    expect(consoleSpy).toHaveBeenCalledTimes(2);
    expect(consoleSpy).toHaveBeenLastCalledWith("Route error", {
      message: "second",
      digest: "d2",
    });
  });
});
