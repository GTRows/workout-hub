import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { PrToast } from "@/components/pr-toast";

describe("PrToast", () => {
  it("renders the message inside an aria-live region", () => {
    const onDismiss = vi.fn();
    render(<PrToast message="New PR: Bench Press" onDismiss={onDismiss} />);
    const toast = screen.getByTestId("pr-toast");
    expect(toast).toBeInTheDocument();
    expect(toast).toHaveAttribute("role", "status");
    expect(toast).toHaveAttribute("aria-live", "polite");
    expect(screen.getByText(/Bench Press/)).toBeInTheDocument();
  });

  it("schedules a dismissal timer that calls onDismiss", () => {
    const setTimeoutSpy = vi.spyOn(window, "setTimeout");
    const onDismiss = vi.fn();
    render(<PrToast message="New PR" onDismiss={onDismiss} />);
    const calls = setTimeoutSpy.mock.calls;
    const matched = calls.find((c) => c[1] === 3000);
    expect(matched, "should schedule a 3s setTimeout").toBeTruthy();
    setTimeoutSpy.mockRestore();
  });
});
