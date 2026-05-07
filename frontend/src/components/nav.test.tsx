import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { NextIntlClientProvider } from "next-intl";
import type { ReactElement } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

const replaceMock = vi.fn();
const pushMock = vi.fn();
vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: pushMock, replace: replaceMock, refresh: vi.fn() }),
  usePathname: () => "/dashboard",
}));

import { Nav } from "@/components/nav";
import {
  clearTokens,
  getAccessToken,
  getRefreshToken,
  setAccessToken,
  setRefreshToken,
} from "@/lib/auth/token-store";

const messages = {
  nav: {
    dashboard: "Dashboard",
    plan: "Plan",
    history: "History",
    exercises: "Exercises",
    insights: "Insights",
    prs: "PRs",
    achievements: "Achievements",
    nutrition: "Nutrition",
    metrics: "Metrics",
    profile: "Profile",
    export: "Export",
    logout: "Log out",
  },
};

function renderNav(ui: ReactElement) {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return render(
    <NextIntlClientProvider locale="en" messages={messages}>
      <QueryClientProvider client={client}>{ui}</QueryClientProvider>
    </NextIntlClientProvider>
  );
}

describe("Nav", () => {
  beforeEach(() => {
    clearTokens();
    window.localStorage.clear();
    replaceMock.mockReset();
    pushMock.mockReset();
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("renders the configured nav links", () => {
    renderNav(<Nav />);
    expect(screen.getByRole("link", { name: /dashboard/i })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /^plan$/i })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /history/i })).toBeInTheDocument();
  });

  it("renders the logout button next to the theme toggle", () => {
    renderNav(<Nav />);
    const logoutButton = screen.getByTestId("logout-button");
    expect(logoutButton).toBeInTheDocument();
    expect(logoutButton).toHaveAttribute("aria-label", "Log out");
    // Theme toggle sits in the same group container.
    expect(screen.getByTestId("theme-toggle")).toBeInTheDocument();
  });

  it("clears tokens, clears query cache, and replaces route on logout click", async () => {
    setAccessToken("a");
    setRefreshToken("r");
    expect(getAccessToken()).toBe("a");
    expect(getRefreshToken()).toBe("r");

    renderNav(<Nav />);
    const user = userEvent.setup();
    await user.click(screen.getByTestId("logout-button"));

    await waitFor(() => expect(replaceMock).toHaveBeenCalledWith("/login"));
    expect(getAccessToken()).toBeNull();
    expect(getRefreshToken()).toBeNull();
  });
});
