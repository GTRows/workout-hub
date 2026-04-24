import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { NextIntlClientProvider } from "next-intl";
import type { ReactElement } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: vi.fn(), replace: vi.fn(), refresh: vi.fn() }),
  useSearchParams: () => new URLSearchParams(),
  usePathname: () => "/export/ai/apply",
}));

import { AiApplyClient } from "@/app/(app)/export/ai/apply/ai-apply-client";
import { clearTokens } from "@/lib/auth/token-store";

const messages = {
  ai: {
    title: "t",
    cheatsheetTitle: "",
    cheatsheetIntro: "",
    step1: "",
    step2: "",
    step3: "",
    step4: "",
    examplePrompt: "",
    pickerTitle: "",
    pickerDescription: "",
    pending: "",
    error: "",
    section: {
      profile: "", plans: "", sessions: "", metrics: "", supplements: "",
    },
    applyTitle: "Apply an AI response",
    applyDescription: "",
    applyButton: "",
    uploadTitle: "Upload",
    uploadDescription: "",
    uploadLabel: "Pick JSON file",
    fileChosen: "Selected: {name}",
    parseError: "Invalid JSON file.",
    parseErrorSchema: "schemaVersion must be 1.",
    diffTitle: "Preview",
    diffDescription: "",
    diffProfile: "Profile: {state}",
    diffPlans: "Plans to write: {count}",
    diffSessions: "Sessions to write: {count}",
    diffMetrics: "Body metrics to write: {count}",
    diffSupplements: "Supplements to write: {count}",
    committing: "Committing...",
    commitButton: "Commit",
    commitError: "Commit failed.",
    cancelButton: "Cancel",
    committedTitle: "Committed",
    committedSummary: "Wrote {metrics} m, {supplements} s, {plans} p, {sessions} ss.",
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

const SAMPLE = {
  schemaVersion: 1,
  exportedAt: "2026-04-23T00:00:00Z",
  user: null,
  plans: [],
  sessions: [],
  bodyMetrics: [{ id: "x", recordedDate: "2026-04-20", weightKg: 78 }],
  supplements: [{ id: "y", name: "Creatine", timing: "morning", active: true }],
};

describe("AiApplyClient", () => {
  beforeEach(() => {
    clearTokens();
    window.localStorage.clear();
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  it("state 2: shows a diff preview after a valid file is uploaded", async () => {
    renderClient(<AiApplyClient />);
    const file = new File([JSON.stringify(SAMPLE)], "dump.json", {
      type: "application/json",
    });
    await userEvent.upload(screen.getByLabelText("Pick JSON file"), file);

    const diff = await screen.findByTestId("ai-apply-diff");
    expect(diff).toHaveTextContent("Body metrics to write: 1");
    expect(diff).toHaveTextContent("Supplements to write: 1");
  });

  it("state 3: commits to /api/export/import and shows the committed summary", async () => {
    let posted: unknown = null;
    vi.stubGlobal(
      "fetch",
      vi.fn(async (input: string | URL | Request, init?: RequestInit) => {
        const url = typeof input === "string" ? input : input.toString();
        if (url.endsWith("/api/export/import") && init?.method === "POST") {
          posted = JSON.parse(init.body as string);
          return jsonResponse(200, {
            profileUpdated: 0,
            metricsInserted: 1,
            supplementsInserted: 1,
            plansInserted: 0,
            sessionsInserted: 0,
            userEmail: "x@test.local",
          });
        }
        throw new Error("unexpected fetch: " + url);
      })
    );

    renderClient(<AiApplyClient />);
    const file = new File([JSON.stringify(SAMPLE)], "dump.json", {
      type: "application/json",
    });
    await userEvent.upload(screen.getByLabelText("Pick JSON file"), file);
    await screen.findByTestId("ai-apply-diff");

    await userEvent.click(screen.getByTestId("ai-apply-commit"));

    await waitFor(() => expect(posted).not.toBeNull());
    expect((posted as { schemaVersion: number }).schemaVersion).toBe(1);
    await screen.findByTestId("ai-apply-committed");
  });

  it("rejects non-JSON files with parseError", async () => {
    renderClient(<AiApplyClient />);
    const bad = new File(["not-json"], "bad.json", {
      type: "application/json",
    });
    await userEvent.upload(screen.getByLabelText("Pick JSON file"), bad);

    await screen.findByText("Invalid JSON file.");
  });
});
