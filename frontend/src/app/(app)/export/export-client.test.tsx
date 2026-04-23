import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { NextIntlClientProvider } from "next-intl";
import type { ReactElement } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: vi.fn(), replace: vi.fn(), refresh: vi.fn() }),
  useSearchParams: () => new URLSearchParams(),
  usePathname: () => "/export",
}));

import { ExportClient } from "@/app/(app)/export/export-client";
import { clearTokens } from "@/lib/auth/token-store";

const messages = {
  export: {
    title: "Export",
    claudeTitle: "Claude summary JSON",
    claudeDescription: "Download last {days} days",
    daysLabel: "Day range",
    downloadButton: "Download summary",
    downloading: "Preparing...",
    error: "Download failed",
    lastDownloadedAt: "Last downloaded: {time}",
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

describe("ExportClient", () => {
  beforeEach(() => {
    clearTokens();
    window.localStorage.clear();
    // jsdom does not implement Blob URL helpers; the download flow needs
    // both so we stub them for the duration of the test.
    if (typeof URL.createObjectURL !== "function") {
      (URL as unknown as { createObjectURL: typeof URL.createObjectURL })
        .createObjectURL = vi.fn(() => "blob:test");
    }
    if (typeof URL.revokeObjectURL !== "function") {
      (URL as unknown as { revokeObjectURL: typeof URL.revokeObjectURL })
        .revokeObjectURL = vi.fn();
    }
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  it("fetches the claude summary and triggers a browser download with the correct filename", async () => {
    const summaryBody = {
      user: { displayName: "X", email: "x@y.z", heightCm: null, weightKg: null, healthNotes: null, goals: null },
      period: { from: "2026-03-24", to: "2026-04-23", days: 30 },
      summary: { totalWorkouts: 1, totalVolumeKg: 1000, avgSessionDurationMin: 45 },
      workouts: [],
    };
    vi.stubGlobal(
      "fetch",
      vi.fn(async (input: string | URL | Request) => {
        const url = typeof input === "string" ? input : input.toString();
        if (url.includes("/api/export/claude-summary")) {
          return jsonResponse(200, summaryBody);
        }
        throw new Error("unexpected fetch: " + url);
      })
    );

    // Capture the download by spying on anchor.click.
    const clickSpy = vi.fn();
    const origCreateElement = document.createElement.bind(document);
    const createElementSpy = vi.spyOn(document, "createElement");
    let capturedAnchor: HTMLAnchorElement | null = null;
    createElementSpy.mockImplementation((tag: string) => {
      const el = origCreateElement(tag);
      if (tag === "a") {
        capturedAnchor = el as HTMLAnchorElement;
        (el as HTMLAnchorElement).click = clickSpy;
      }
      return el;
    });

    renderClient(<ExportClient />);
    const user = userEvent.setup();
    await user.click(screen.getByRole("button", { name: /download summary/i }));

    await waitFor(() => expect(clickSpy).toHaveBeenCalled());
    expect(capturedAnchor).not.toBeNull();
    expect(capturedAnchor!.download).toMatch(/^workouthub-claude-\d{4}-\d{2}-\d{2}\.json$/);
    expect(await screen.findByText(/last downloaded/i)).toBeInTheDocument();
  });

  it("shows error message when the request fails", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => jsonResponse(500, { message: "boom" }))
    );

    renderClient(<ExportClient />);
    const user = userEvent.setup();
    await user.click(screen.getByRole("button", { name: /download summary/i }));

    expect(await screen.findByText(/download failed/i)).toBeInTheDocument();
  });

  it("sends the chosen days value in the query string", async () => {
    let lastUrl = "";
    vi.stubGlobal(
      "fetch",
      vi.fn(async (input: string | URL | Request) => {
        const url = typeof input === "string" ? input : input.toString();
        lastUrl = url;
        return jsonResponse(200, {
          user: { displayName: "X", email: "x@y.z", heightCm: null, weightKg: null, healthNotes: null, goals: null },
          period: { from: "2026-01-01", to: "2026-04-23", days: 90 },
          summary: { totalWorkouts: 0, totalVolumeKg: 0, avgSessionDurationMin: null },
          workouts: [],
        });
      })
    );

    const clickSpy = vi.fn();
    const origCreateElement = document.createElement.bind(document);
    vi.spyOn(document, "createElement").mockImplementation((tag: string) => {
      const el = origCreateElement(tag);
      if (tag === "a") {
        (el as HTMLAnchorElement).click = clickSpy;
      }
      return el;
    });

    renderClient(<ExportClient />);
    const user = userEvent.setup();
    // fireEvent.change fires a single change event with value="90" instead
    // of userEvent.type's keystroke stream, which on a controlled number
    // input would concatenate against the current state.
    fireEvent.change(screen.getByLabelText(/day range/i), {
      target: { value: "90" },
    });
    await user.click(screen.getByRole("button", { name: /download summary/i }));

    await waitFor(() => expect(lastUrl).toContain("days=90"));
  });
});
