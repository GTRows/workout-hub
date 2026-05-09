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
    fullTitle: "Download full JSON",
    fullDescription: "Full dump",
    fullButton: "Download full JSON",
    fullDownloading: "Preparing...",
    fullError: "Full export failed",
    importTitle: "Restore from file",
    importDescription: "Upload previous dump",
    importFileLabel: "Pick JSON file",
    importUploading: "Uploading...",
    importError: "Restore failed",
    importParseError: "Invalid JSON file",
    importSuccess: "Success: wrote {metrics} metrics, {supplements} supplements, {plans} plans, {sessions} sessions.",
    sectionTitle: "Download a single section",
    sectionDescription: "Download just one slice",
    sectionPending: "Preparing...",
    sectionError: "Section download failed for {section}.",
    sectionButton: {
      profile: "Profile",
      plans: "Plans",
      sessions: "Sessions",
      metrics: "Metrics",
      supplements: "Supplements",
    },
    importWarningsHeading: "Warnings",
    importSuggestionsHeading: "Suggestions",
    csvButton: "Download sessions.csv (Strong)",
    csvPending: "Preparing...",
    csvError: "CSV export failed.",
    csvHint: "Strong-compatible.",
    importConfirm: "Importing {file} will replace your data. Continue?",
    historyTitle: "Download history",
    historyDescription: "Last 10 export actions on this device.",
    historyEmpty: "No previous exports yet.",
    historyClear: "Clear history",
    historyKind: {
      claude: "Claude summary",
      full: "Full JSON dump",
      section: "Section",
      csv: "CSV",
    },
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
    vi.spyOn(window, "confirm").mockReturnValue(true);
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

  it("downloads the Strong-compatible sessions CSV when csv-sessions is clicked", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async (input: string | URL | Request) => {
        const url = typeof input === "string" ? input : input.toString();
        if (url.endsWith("/api/export/csv/sessions")) {
          return new Response(
            "Date,Workout Name,Exercise Name,Set Order,Weight,Reps,Notes,Workout Notes\n",
            { status: 200, headers: { "content-type": "text/csv" } }
          );
        }
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

    renderClient(<ExportClient />);
    const user = userEvent.setup();
    await user.click(screen.getByTestId("csv-sessions"));

    await waitFor(() => expect(clickSpy).toHaveBeenCalled());
    expect(captured).not.toBeNull();
    expect(captured!.download).toMatch(
      /^workouthub-sessions-\d{4}-\d{2}-\d{2}\.csv$/
    );
  });

  it("downloads the metrics section when its section button is clicked", async () => {
    const sectionBody = [
      { id: "ffffffff-1111-1111-1111-111111111111", recordedDate: "2026-04-20", weightKg: 78 },
    ];
    let hitUrl = "";
    vi.stubGlobal(
      "fetch",
      vi.fn(async (input: string | URL | Request) => {
        const url = typeof input === "string" ? input : input.toString();
        hitUrl = url;
        if (url.endsWith("/api/export/metrics"))
          return jsonResponse(200, sectionBody);
        throw new Error("unexpected fetch: " + url);
      })
    );

    const clickSpy = vi.fn();
    const origCreateElement = document.createElement.bind(document);
    let capturedAnchor: HTMLAnchorElement | null = null;
    vi.spyOn(document, "createElement").mockImplementation((tag: string) => {
      const el = origCreateElement(tag);
      if (tag === "a") {
        capturedAnchor = el as HTMLAnchorElement;
        (el as HTMLAnchorElement).click = clickSpy;
      }
      return el;
    });

    renderClient(<ExportClient />);
    const user = userEvent.setup();
    await user.click(screen.getByTestId("section-metrics"));

    await waitFor(() => expect(clickSpy).toHaveBeenCalled());
    expect(hitUrl).toContain("/api/export/metrics");
    expect(capturedAnchor).not.toBeNull();
    expect(capturedAnchor!.download).toMatch(
      /^workouthub-metrics-\d{4}-\d{2}-\d{2}\.json$/
    );
  });

  it("downloads the full JSON dump when the full button is clicked", async () => {
    const fullBody = {
      schemaVersion: 1,
      exportedAt: "2026-04-23T00:00:00Z",
      user: {
        id: "11111111-1111-4111-8111-111111111111",
        email: "u@e.com",
        displayName: "Tester",
        heightCm: null,
        weightKg: null,
        birthDate: null,
        gender: null,
        healthNotes: null,
        goals: null,
      },
      plans: [],
      sessions: [],
      bodyMetrics: [],
      supplements: [],
    };
    vi.stubGlobal(
      "fetch",
      vi.fn(async (input: string | URL | Request) => {
        const url = typeof input === "string" ? input : input.toString();
        if (url.endsWith("/api/export/full")) return jsonResponse(200, fullBody);
        throw new Error("unexpected fetch: " + url);
      })
    );

    const clickSpy = vi.fn();
    const origCreateElement = document.createElement.bind(document);
    let capturedAnchor: HTMLAnchorElement | null = null;
    vi.spyOn(document, "createElement").mockImplementation((tag: string) => {
      const el = origCreateElement(tag);
      if (tag === "a") {
        capturedAnchor = el as HTMLAnchorElement;
        (el as HTMLAnchorElement).click = clickSpy;
      }
      return el;
    });

    renderClient(<ExportClient />);
    const user = userEvent.setup();
    await user.click(screen.getByRole("button", { name: "Download full JSON" }));

    await waitFor(() => expect(clickSpy).toHaveBeenCalled());
    expect(capturedAnchor).not.toBeNull();
    expect(capturedAnchor!.download).toMatch(
      /^workouthub-full-\d{4}-\d{2}-\d{2}\.json$/
    );
  });

  it("uploads a JSON file and shows the import success message", async () => {
    let posted: unknown = null;
    vi.stubGlobal(
      "fetch",
      vi.fn(async (input: string | URL | Request, init?: RequestInit) => {
        const url = typeof input === "string" ? input : input.toString();
        if (url.endsWith("/api/export/import")) {
          posted = JSON.parse(init!.body as string);
          return jsonResponse(200, {
            profileUpdated: 1,
            metricsInserted: 2,
            supplementsInserted: 3,
            plansInserted: 1,
            sessionsInserted: 4,
            userEmail: "x@test.local",
          });
        }
        throw new Error("unexpected fetch: " + url);
      })
    );

    renderClient(<ExportClient />);
    const dump = JSON.stringify({
      schemaVersion: 1,
      exportedAt: "2026-04-23T00:00:00Z",
      user: null,
      plans: [],
      sessions: [],
      bodyMetrics: [],
      supplements: [],
    });
    const file = new File([dump], "dump.json", { type: "application/json" });
    const input = screen.getByLabelText("Pick JSON file") as HTMLInputElement;
    await userEvent.upload(input, file);

    await waitFor(() => expect(posted).not.toBeNull());
    expect((posted as { schemaVersion: number }).schemaVersion).toBe(1);
    await screen.findByTestId("import-success");
  });

  it("surfaces warnings and suggestions returned by the import endpoint", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async (input: string | URL | Request, init?: RequestInit) => {
        const url = typeof input === "string" ? input : input.toString();
        if (url.endsWith("/api/export/import") && init?.method === "POST") {
          return jsonResponse(200, {
            profileUpdated: 0,
            metricsInserted: 0,
            supplementsInserted: 1,
            plansInserted: 0,
            sessionsInserted: 0,
            userEmail: "x@test.local",
            warnings: ["supplement 'X' has unknown timing 'midnight_snack'"],
            suggestions: ["payload has 1001 sessions; some LLMs will truncate."],
          });
        }
        throw new Error("unexpected fetch: " + url);
      })
    );

    renderClient(<ExportClient />);
    const dump = JSON.stringify({
      schemaVersion: 1,
      exportedAt: "2026-04-23T00:00:00Z",
      user: null,
      plans: [],
      sessions: [],
      bodyMetrics: [],
      supplements: [],
    });
    const file = new File([dump], "dump.json", { type: "application/json" });
    const input = screen.getByLabelText("Pick JSON file") as HTMLInputElement;
    await userEvent.upload(input, file);

    await screen.findByTestId("import-warnings");
    expect(screen.getByTestId("import-warnings")).toHaveTextContent(
      "midnight_snack"
    );
    expect(screen.getByTestId("import-suggestions")).toHaveTextContent(
      "1001 sessions"
    );
  });

  it("shows an import error when the server rejects the upload", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async (input: string | URL | Request) => {
        const url = typeof input === "string" ? input : input.toString();
        if (url.endsWith("/api/export/import")) {
          return jsonResponse(422, {
            timestamp: "2026-04-23T00:00:00Z",
            status: 422,
            error: "unprocessable_entity",
            message: "Importing plans and sessions is not supported yet",
            path: "/api/export/import",
          });
        }
        throw new Error("unexpected fetch: " + url);
      })
    );

    renderClient(<ExportClient />);
    const dump = JSON.stringify({
      schemaVersion: 1,
      exportedAt: "2026-04-23T00:00:00Z",
      user: null,
      plans: [{ id: "ffffffff-1111-1111-1111-111111111111", name: "P", active: true, days: [] }],
      sessions: [],
      bodyMetrics: [],
      supplements: [],
    });
    const file = new File([dump], "dump.json", { type: "application/json" });
    const input = screen.getByLabelText("Pick JSON file") as HTMLInputElement;
    await userEvent.upload(input, file);

    const alerts = await screen.findAllByRole("alert");
    expect(alerts.length).toBeGreaterThan(0);
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

  it("aborts the import when the user cancels the confirm prompt", async () => {
    const confirmSpy = vi.spyOn(window, "confirm").mockReturnValue(false);
    const fetchSpy = vi.fn(async () => jsonResponse(200, {}));
    vi.stubGlobal("fetch", fetchSpy);

    renderClient(<ExportClient />);
    const dump = JSON.stringify({
      schemaVersion: 1,
      exportedAt: "2026-04-23T00:00:00Z",
      user: null,
      plans: [],
      sessions: [],
      bodyMetrics: [],
      supplements: [],
    });
    const file = new File([dump], "rejected-dump.json", {
      type: "application/json",
    });
    const input = screen.getByLabelText("Pick JSON file") as HTMLInputElement;
    await userEvent.upload(input, file);

    expect(confirmSpy).toHaveBeenCalled();
    const args = confirmSpy.mock.calls[0]?.[0] ?? "";
    expect(args).toContain("rejected-dump.json");
    const importCalls = fetchSpy.mock.calls.filter((c) => {
      const url =
        typeof c[0] === "string" ? c[0] : (c[0] as URL | Request).toString();
      return url.endsWith("/api/export/import");
    });
    expect(importCalls).toHaveLength(0);
    expect(screen.queryByTestId("import-success")).toBeNull();
  });

  it("proceeds with the import when the user confirms the prompt", async () => {
    vi.spyOn(window, "confirm").mockReturnValue(true);
    vi.stubGlobal(
      "fetch",
      vi.fn(async (input: string | URL | Request, init?: RequestInit) => {
        const url = typeof input === "string" ? input : input.toString();
        if (url.endsWith("/api/export/import") && init?.method === "POST") {
          return jsonResponse(200, {
            profileUpdated: 1,
            metricsInserted: 2,
            supplementsInserted: 3,
            plansInserted: 1,
            sessionsInserted: 4,
            userEmail: "x@test.local",
          });
        }
        throw new Error("unexpected fetch: " + url);
      }),
    );

    renderClient(<ExportClient />);
    const dump = JSON.stringify({
      schemaVersion: 1,
      exportedAt: "2026-04-23T00:00:00Z",
      user: null,
      plans: [],
      sessions: [],
      bodyMetrics: [],
      supplements: [],
    });
    const file = new File([dump], "ok-dump.json", {
      type: "application/json",
    });
    const input = screen.getByLabelText("Pick JSON file") as HTMLInputElement;
    await userEvent.upload(input, file);

    await screen.findByTestId("import-success");
  });

  it("appends a download history entry after a successful claude summary download", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async (input: string | URL | Request) => {
        const url = typeof input === "string" ? input : input.toString();
        if (url.includes("/api/export/claude-summary")) {
          return jsonResponse(200, {
            user: {
              displayName: "X",
              email: "x@y.z",
              heightCm: null,
              weightKg: null,
              healthNotes: null,
              goals: null,
            },
            period: { from: "2026-03-24", to: "2026-04-23", days: 30 },
            summary: {
              totalWorkouts: 0,
              totalVolumeKg: 0,
              avgSessionDurationMin: null,
            },
            workouts: [],
          });
        }
        throw new Error("unexpected fetch: " + url);
      }),
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
    await user.click(screen.getByRole("button", { name: /download summary/i }));

    await screen.findByTestId("history-list");
    expect(screen.getByTestId("history-list")).toHaveTextContent(
      "Claude summary",
    );
  });
});
