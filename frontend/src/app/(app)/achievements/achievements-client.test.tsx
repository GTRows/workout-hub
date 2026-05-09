import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import { NextIntlClientProvider } from "next-intl";
import type { ReactElement } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: vi.fn(), replace: vi.fn(), refresh: vi.fn() }),
  useSearchParams: () => new URLSearchParams(),
  usePathname: () => "/achievements",
}));

import { AchievementsClient } from "@/app/(app)/achievements/achievements-client";
import { clearTokens } from "@/lib/auth/token-store";

const messages = {
  achievements: {
    title: "Achievements",
    loading: "Loading...",
    error: "Could not load achievements.",
    summary: "{unlocked} of {total} unlocked",
    unlockedHeading: "Unlocked",
    unlockedEmpty: "No achievements yet.",
    nextHeading: "Next up",
    nextEmpty: "All unlocked.",
    unlockedAt: "Unlocked: {time}",
    threshold: "{rule}: {value}",
    rule: {
      session_count: "Finished workouts",
      streak_days: "Streak days",
      pr_count: "Personal records",
      volume_kg: "Total volume (kg)",
    },
  },
  common: {
    loading: "Loading",
  },
};

function renderClient(ui: ReactElement) {
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

const FIRST_WORKOUT = {
  id: "a0000001-0000-0000-0000-000000000001",
  code: "first-workout",
  nameTr: "Ilk antrenman",
  nameEn: "First workout",
  descriptionEn: "Finished your first workout.",
  descriptionTr: "Ilk antrenman.",
  icon: "trophy",
  ruleType: "session_count",
  threshold: 1,
  unlocked: true,
  unlockedAt: "2026-04-23T10:00:00Z",
  progressValue: 1,
};

const TEN_WORKOUTS = {
  id: "a0000001-0000-0000-0000-000000000002",
  code: "ten-workouts",
  nameTr: "10 antrenman",
  nameEn: "10 workouts",
  descriptionEn: "Crossed 10 finished workouts.",
  descriptionTr: "10 antrenman.",
  icon: "trophy",
  ruleType: "session_count",
  threshold: 10,
  unlocked: false,
  unlockedAt: null,
  progressValue: null,
};

describe("AchievementsClient", () => {
  beforeEach(() => {
    clearTokens();
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  it("renders unlocked and locked sections from the API", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async (input: string | URL | Request) => {
        const url = typeof input === "string" ? input : input.toString();
        if (url.includes("/api/achievements/me")) {
          return jsonResponse(200, [FIRST_WORKOUT, TEN_WORKOUTS]);
        }
        throw new Error("unexpected fetch: " + url);
      })
    );

    renderClient(<AchievementsClient />);
    await waitFor(() =>
      expect(screen.getByTestId("ach-card-first-workout")).toBeInTheDocument()
    );
    expect(screen.getByText(/First workout/)).toBeInTheDocument();
    expect(screen.getByText(/10 workouts/)).toBeInTheDocument();
    expect(screen.getByTestId("ach-summary").textContent).toContain("1 of 2");
  });

  it("shows the unlockedEmpty message when nothing is unlocked yet", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async (input: string | URL | Request) => {
        const url = typeof input === "string" ? input : input.toString();
        if (url.includes("/api/achievements/me")) {
          return jsonResponse(200, [TEN_WORKOUTS]);
        }
        throw new Error("unexpected fetch: " + url);
      })
    );

    renderClient(<AchievementsClient />);
    await waitFor(() =>
      expect(screen.getByText("No achievements yet.")).toBeInTheDocument()
    );
  });
});
