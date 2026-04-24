import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { NextIntlClientProvider } from "next-intl";
import type { ReactElement } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: vi.fn(), replace: vi.fn(), refresh: vi.fn() }),
  useSearchParams: () => new URLSearchParams(),
  usePathname: () => "/export/ai",
}));

import { AiPrepareClient } from "@/app/(app)/export/ai/ai-prepare-client";
import { clearTokens } from "@/lib/auth/token-store";

const messages = {
  ai: {
    title: "AI roundtrip",
    cheatsheetTitle: "How this works",
    cheatsheetIntro: "Intro",
    step1: "Step 1",
    step2: "Step 2",
    step3: "Step 3",
    step4: "Step 4",
    examplePrompt: "prompt",
    pickerTitle: "Pick",
    pickerDescription: "",
    pending: "Preparing...",
    error: "Download failed.",
    section: {
      profile: "Profile",
      plans: "Plans",
      sessions: "Sessions",
      metrics: "Metrics",
      supplements: "Supplements",
    },
    applyTitle: "Apply",
    applyDescription: "",
    applyButton: "Open Apply page",
    uploadTitle: "",
    uploadDescription: "",
    uploadLabel: "",
    fileChosen: "",
    parseError: "",
    parseErrorSchema: "",
    diffTitle: "",
    diffDescription: "",
    diffProfile: "",
    diffPlans: "",
    diffSessions: "",
    diffMetrics: "",
    diffSupplements: "",
    committing: "",
    commitButton: "",
    commitError: "",
    cancelButton: "",
    committedTitle: "",
    committedSummary: "",
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

describe("AiPrepareClient (state 1: prepare)", () => {
  beforeEach(() => {
    clearTokens();
    window.localStorage.clear();
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

  it("renders the cheatsheet + section picker + link to apply page", () => {
    renderClient(<AiPrepareClient />);
    expect(screen.getByTestId("ai-cheatsheet")).toBeInTheDocument();
    expect(screen.getByTestId("ai-section-picker")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Open Apply page" }))
        .toHaveAttribute("href", "/export/ai/apply");
  });

  it("downloads the selected section JSON", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async (input: string | URL | Request) => {
        const url = typeof input === "string" ? input : input.toString();
        if (url.endsWith("/api/export/metrics"))
          return jsonResponse(200, [{ id: "x", recordedDate: "2026-04-20" }]);
        throw new Error("unexpected fetch: " + url);
      })
    );

    const clickSpy = vi.fn();
    const origCreate = document.createElement.bind(document);
    let captured: HTMLAnchorElement | null = null;
    vi.spyOn(document, "createElement").mockImplementation((tag: string) => {
      const el = origCreate(tag);
      if (tag === "a") {
        captured = el as HTMLAnchorElement;
        (el as HTMLAnchorElement).click = clickSpy;
      }
      return el;
    });

    renderClient(<AiPrepareClient />);
    await userEvent.click(screen.getByTestId("ai-section-metrics"));

    await waitFor(() => expect(clickSpy).toHaveBeenCalled());
    expect(captured).not.toBeNull();
    expect(captured!.download).toMatch(
      /^workouthub-metrics-\d{4}-\d{2}-\d{2}\.json$/
    );
  });
});
