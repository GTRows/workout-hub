import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { NextIntlClientProvider } from "next-intl";
import type { ReactElement } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: vi.fn(), replace: vi.fn(), refresh: vi.fn() }),
  useSearchParams: () => new URLSearchParams(),
  usePathname: () => "/export",
}));

import { HealthImportClient } from "@/app/(app)/export/health-import-client";
import { clearTokens } from "@/lib/auth/token-store";

const messages = {
  health: {
    appleTitle: "Apple Health import",
    appleDescription: "desc",
    toggleBodyMass: "Body Mass",
    toggleWorkouts: "Workouts",
    filePickerLabel: "Pick file",
    uploading: "Uploading...",
    imported: "Imported: {bm} weight, {wo} workouts.",
    skipped: "Skipped: {bm} weight, {wo} workouts.",
    error: "Import failed.",
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

describe("HealthImportClient", () => {
  beforeEach(() => {
    clearTokens();
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  it("uploads the file and shows the imported counts", async () => {
    let postedUrl: string | null = null;
    vi.stubGlobal(
      "fetch",
      vi.fn(async (input: string | URL | Request, init?: RequestInit) => {
        const url = typeof input === "string" ? input : input.toString();
        const method = init?.method ?? "GET";
        if (url.includes("/api/health/import/apple") && method === "POST") {
          postedUrl = url;
          return jsonResponse(200, {
            bodyMassParsed: 2,
            bodyMassImported: 2,
            bodyMassSkipped: 0,
            workoutsParsed: 1,
            workoutsImported: 1,
            workoutsSkipped: 0,
          });
        }
        throw new Error("unexpected fetch: " + url + " " + method);
      })
    );

    renderClient(<HealthImportClient />);

    const file = new File(["<HealthData/>"], "export.xml", {
      type: "application/xml",
    });
    const input = screen.getByTestId("apple-file-input") as HTMLInputElement;
    fireEvent.change(input, { target: { files: [file] } });

    await waitFor(() =>
      expect(screen.getByTestId("apple-import-result")).toBeInTheDocument()
    );
    expect(screen.getByText(/Imported: 2 weight, 1 workouts/)).toBeInTheDocument();
    expect(postedUrl).toContain("bodyMass=true");
    expect(postedUrl).toContain("workouts=true");
  });

  it("respects the body-mass toggle in the upload querystring", async () => {
    let postedUrl: string | null = null;
    vi.stubGlobal(
      "fetch",
      vi.fn(async (input: string | URL | Request, init?: RequestInit) => {
        const url = typeof input === "string" ? input : input.toString();
        const method = init?.method ?? "GET";
        if (url.includes("/api/health/import/apple") && method === "POST") {
          postedUrl = url;
          return jsonResponse(200, {
            bodyMassParsed: 0,
            bodyMassImported: 0,
            bodyMassSkipped: 0,
            workoutsParsed: 1,
            workoutsImported: 1,
            workoutsSkipped: 0,
          });
        }
        throw new Error("unexpected fetch: " + url + " " + method);
      })
    );

    renderClient(<HealthImportClient />);
    fireEvent.click(screen.getByTestId("apple-toggle-body-mass"));

    const file = new File(["<HealthData/>"], "export.xml", {
      type: "application/xml",
    });
    fireEvent.change(screen.getByTestId("apple-file-input"), {
      target: { files: [file] },
    });

    await waitFor(() => expect(postedUrl).not.toBeNull());
    expect(postedUrl).toContain("bodyMass=false");
    expect(postedUrl).toContain("workouts=true");
  });
});
