import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { NextIntlClientProvider } from "next-intl";
import type { ReactElement } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: vi.fn(), replace: vi.fn(), refresh: vi.fn() }),
  useSearchParams: () => new URLSearchParams(),
  usePathname: () => "/metrics",
}));

// Recharts relies on ResizeObserver / layout measurements that jsdom does
// not provide; stub the ResponsiveContainer so the LineChart still renders
// into the test DOM with a fixed size.
vi.mock("recharts", async () => {
  const actual = await vi.importActual<typeof import("recharts")>("recharts");
  return {
    ...actual,
    ResponsiveContainer: ({ children }: { children: React.ReactNode }) => (
      <div style={{ width: 600, height: 300 }} data-testid="recharts-container">
        {children}
      </div>
    ),
  };
});

import { MetricsClient } from "@/app/(app)/metrics/metrics-client";
import { clearTokens } from "@/lib/auth/token-store";

const messages = {
  metrics: {
    title: "Body metrics",
    loading: "Loading...",
    empty: "No entries yet",
    recordedDate: "Date",
    weightKg: "Weight (kg)",
    bodyFatPercent: "Body fat (%)",
    waistCm: "Waist (cm)",
    chestCm: "Chest (cm)",
    armCm: "Arm (cm)",
    thighCm: "Thigh (cm)",
    notes: "Notes",
    saveButton: "Save",
    saving: "Saving...",
    chartTitle: "Weight chart",
    range: { week: "7 days", month: "30 days", all: "All" },
    saveSuccess: "Saved",
    deleteAction: "Delete",
    deleteConfirm: "Delete this entry?",
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

function metric(id: string, recordedDate: string, weightKg: number | null) {
  return {
    id,
    recordedDate,
    weightKg,
    bodyFatPercent: null,
    waistCm: null,
    chestCm: null,
    armCm: null,
    thighCm: null,
    photoUrl: null,
    notes: null,
    createdAt: `${recordedDate}T00:00:00Z`,
    updatedAt: `${recordedDate}T00:00:00Z`,
  };
}

describe("MetricsClient", () => {
  beforeEach(() => {
    clearTokens();
    window.localStorage.clear();
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  it("renders the weight chart when metrics are available", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async (input: string | URL | Request) => {
        const url = typeof input === "string" ? input : input.toString();
        if (url.endsWith("/api/metrics")) {
          return jsonResponse(200, [
            metric("ffffffff-1111-1111-1111-111111111111", "2026-04-10", 78.5),
            metric("ffffffff-2222-2222-2222-222222222222", "2026-04-15", 77.8),
          ]);
        }
        throw new Error("unexpected fetch: " + url);
      })
    );

    renderClient(<MetricsClient />);
    await waitFor(() =>
      expect(screen.getByTestId("recharts-container")).toBeInTheDocument()
    );
  });

  it("posts the save form and refreshes the list", async () => {
    let posted: unknown = null;
    let getCallCount = 0;
    vi.stubGlobal(
      "fetch",
      vi.fn(async (input: string | URL | Request, init?: RequestInit) => {
        const url = typeof input === "string" ? input : input.toString();
        const method = init?.method ?? "GET";
        if (url.endsWith("/api/metrics") && method === "GET") {
          getCallCount++;
          if (getCallCount === 1) return jsonResponse(200, []);
          return jsonResponse(
            200,
            [metric("ffffffff-3333-3333-3333-333333333333", "2026-04-23", 78.0)]
          );
        }
        if (url.endsWith("/api/metrics") && method === "POST") {
          posted = JSON.parse(init!.body as string);
          return jsonResponse(
            201,
            metric("ffffffff-3333-3333-3333-333333333333", "2026-04-23", 78.0)
          );
        }
        throw new Error("unexpected fetch: " + url + " " + method);
      })
    );

    renderClient(<MetricsClient />);
    await screen.findByText(/no entries yet/i);

    const user = userEvent.setup();
    fireEvent.change(screen.getByLabelText(/Weight/i), {
      target: { value: "78.0" },
    });
    await user.click(screen.getByRole("button", { name: "Save" }));

    await waitFor(() => expect(posted).not.toBeNull());
    expect(posted).toMatchObject({ weightKg: 78 });
    // Subsequent list query triggers after invalidation.
    await waitFor(() =>
      expect(screen.getByText("2026-04-23")).toBeInTheDocument()
    );
  });

  it("changes the chart range when a range button is pressed", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => jsonResponse(200, []))
    );

    renderClient(<MetricsClient />);
    await screen.findByText(/weight chart/i);

    const user = userEvent.setup();
    await user.click(screen.getByRole("button", { name: "All" }));
    expect(screen.getByRole("button", { name: "All" })).toHaveAttribute(
      "aria-pressed",
      "true"
    );
  });
});
