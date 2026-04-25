import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it } from "vitest";

import { ThemeToggle } from "@/components/theme-toggle";

describe("ThemeToggle", () => {
  beforeEach(() => {
    window.localStorage.clear();
    document.documentElement.classList.remove("dark", "light");
  });

  afterEach(() => {
    window.localStorage.clear();
    document.documentElement.classList.remove("dark", "light");
  });

  it("cycles auto -> dark -> light -> auto and toggles the html class", async () => {
    render(<ThemeToggle />);
    const button = screen.getByTestId("theme-toggle");
    const user = userEvent.setup();

    expect(button.textContent).toBe("Auto");
    expect(document.documentElement.classList.contains("dark")).toBe(false);
    expect(document.documentElement.classList.contains("light")).toBe(false);

    await user.click(button);
    expect(button.textContent).toBe("Dark");
    expect(document.documentElement.classList.contains("dark")).toBe(true);

    await user.click(button);
    expect(button.textContent).toBe("Light");
    expect(document.documentElement.classList.contains("dark")).toBe(false);
    expect(document.documentElement.classList.contains("light")).toBe(true);

    await user.click(button);
    expect(button.textContent).toBe("Auto");
    expect(document.documentElement.classList.contains("dark")).toBe(false);
    expect(document.documentElement.classList.contains("light")).toBe(false);
  });

  it("persists the choice to localStorage and reapplies on remount", async () => {
    const { unmount } = render(<ThemeToggle />);
    const user = userEvent.setup();
    await user.click(screen.getByTestId("theme-toggle"));
    expect(window.localStorage.getItem("wh.theme")).toBe("dark");
    unmount();

    document.documentElement.classList.remove("dark", "light");
    render(<ThemeToggle />);
    expect(document.documentElement.classList.contains("dark")).toBe(true);
    expect(screen.getByTestId("theme-toggle").textContent).toBe("Dark");
  });
});
