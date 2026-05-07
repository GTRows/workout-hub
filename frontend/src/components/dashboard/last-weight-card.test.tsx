import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import { NextIntlClientProvider } from "next-intl";
import type { ReactElement } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { LastWeightCard } from "@/components/dashboard/last-weight-card";

const messages = {
  dashboard: {
    loading: "Loading...",
    lastWeightTitle: "Last weight",
    lastWeightValue: "{kg} kg",
    lastWeightToday: "Today",
    lastWeightDaysAgo: "{days} days ago",
    lastWeightAbsolute: "{date}",
    lastWeightEmpty: "No weight recorded yet",
    lastWeightAddCta: "Log weight",
  },
};

function renderCard(ui: ReactElement) {
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

function routeFetch(routes: Record<string, () => Response>) {
  return vi.fn(async (input: string | URL | Request) => {
    const url = typeof input === "string" ? input : input.toString();
    for (const [suffix, factory] of Object.entries(routes)) {
      if (url.includes(suffix)) return factory();
    }
    throw new Error("unexpected fetch: " + url);
  });
}

function isoDaysAgo(days: number): string {
  const d = new Date();
  d.setHours(0, 0, 0, 0);
  d.setDate(d.getDate() - days);
  const yyyy = d.getFullYear();
  const mm = String(d.getMonth() + 1).padStart(2, "0");
  const dd = String(d.getDate()).padStart(2, "0");
  return `${yyyy}-${mm}-${dd}`;
}

function makeMetric(
  id: string,
  recordedDate: string,
  weightKg: number | null
) {
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
    createdAt: "2026-04-01T00:00:00Z",
    updatedAt: "2026-04-01T00:00:00Z",
  };
}

describe("LastWeightCard", () => {
  beforeEach(() => {
    // no-op
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("renders the latest weight when entries exist", async () => {
    const metrics = [
      makeMetric(
        "11111111-1111-1111-1111-111111111101",
        isoDaysAgo(10),
        72.0
      ),
      makeMetric(
        "11111111-1111-1111-1111-111111111102",
        isoDaysAgo(3),
        78.4
      ),
      makeMetric(
        "11111111-1111-1111-1111-111111111103",
        isoDaysAgo(15),
        70.0
      ),
    ];
    vi.stubGlobal(
      "fetch",
      routeFetch({
        "/api/metrics": () => jsonResponse(200, metrics),
      })
    );

    renderCard(<LastWeightCard />);
    expect(await screen.findByText("78.4 kg")).toBeInTheDocument();
    expect(screen.getByText("3 days ago")).toBeInTheDocument();
  });

  it("renders empty CTA when the metrics list is empty", async () => {
    vi.stubGlobal(
      "fetch",
      routeFetch({
        "/api/metrics": () => jsonResponse(200, []),
      })
    );

    renderCard(<LastWeightCard />);
    expect(
      await screen.findByText("No weight recorded yet")
    ).toBeInTheDocument();
    const cta = screen.getByRole("link", { name: /log weight/i });
    expect(cta).toHaveAttribute("href", "/metrics");
  });

  it("renders empty CTA when no entry has weightKg", async () => {
    const metrics = [
      makeMetric("11111111-1111-1111-1111-111111111101", isoDaysAgo(2), null),
      makeMetric("11111111-1111-1111-1111-111111111102", isoDaysAgo(5), null),
    ];
    vi.stubGlobal(
      "fetch",
      routeFetch({
        "/api/metrics": () => jsonResponse(200, metrics),
      })
    );

    renderCard(<LastWeightCard />);
    expect(
      await screen.findByText("No weight recorded yet")
    ).toBeInTheDocument();
  });

  it("renders absolute date for entries older than 7 days", async () => {
    const metrics = [
      makeMetric(
        "11111111-1111-1111-1111-111111111101",
        isoDaysAgo(30),
        81.2
      ),
    ];
    vi.stubGlobal(
      "fetch",
      routeFetch({
        "/api/metrics": () => jsonResponse(200, metrics),
      })
    );

    renderCard(<LastWeightCard />);
    expect(await screen.findByText("81.2 kg")).toBeInTheDocument();
    expect(screen.queryByText(/days ago/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/today/i)).not.toBeInTheDocument();
  });
});
