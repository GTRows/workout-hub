import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import { NextIntlClientProvider } from "next-intl";
import type { ReactElement } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: vi.fn(), replace: vi.fn(), refresh: vi.fn() }),
  useSearchParams: () => new URLSearchParams(),
  usePathname: () => "/prs",
}));

import { PrsClient } from "@/app/(app)/prs/prs-client";
import { clearTokens } from "@/lib/auth/token-store";

const messages = {
  prs: {
    title: "Personal records",
    loading: "Loading...",
    empty: "No records yet.",
    oneRm: "1RM {value} kg",
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

describe("PrsClient", () => {
  beforeEach(() => {
    clearTokens();
    window.localStorage.clear();
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  it("renders the personal records returned by the API", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async () =>
        jsonResponse(200, [
          {
            exerciseId: "ffffffff-1111-1111-1111-111111111111",
            exerciseNameTr: "Bench Press",
            exerciseNameEn: "Bench Press",
            estimatedOneRmKg: 116.67,
            weightKg: 100,
            repsDone: 5,
            achievedAt: "2026-04-22",
          },
          {
            exerciseId: "ffffffff-2222-2222-2222-222222222222",
            exerciseNameTr: "Squat",
            exerciseNameEn: "Squat",
            estimatedOneRmKg: 150,
            weightKg: 130,
            repsDone: 5,
            achievedAt: "2026-04-20",
          },
        ])
      )
    );

    renderClient(<PrsClient />);

    expect(await screen.findByText("Bench Press")).toBeInTheDocument();
    expect(screen.getByText("Squat")).toBeInTheDocument();
    expect(screen.getByText(/1RM 116.67 kg/)).toBeInTheDocument();
    expect(screen.getByText(/1RM 150 kg/)).toBeInTheDocument();
  });

  it("shows the empty state when no PRs exist", async () => {
    vi.stubGlobal("fetch", vi.fn(async () => jsonResponse(200, [])));

    renderClient(<PrsClient />);
    expect(await screen.findByText("No records yet.")).toBeInTheDocument();
  });
});
