import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { NextIntlClientProvider } from "next-intl";
import type { ReactElement } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: vi.fn(), replace: vi.fn(), refresh: vi.fn() }),
  useSearchParams: () => new URLSearchParams(),
  usePathname: () => "/profile",
}));

import { ProfileClient } from "@/app/(app)/profile/profile-client";
import { clearTokens } from "@/lib/auth/token-store";

const messages = {
  nav: {
    logout: "Log out",
  },
  profile: {
    title: "Profile",
    loading: "Loading...",
    displayName: "Display name",
    email: "Email",
    heightCm: "Height (cm)",
    weightKg: "Weight (kg)",
    birthDate: "Birth date",
    gender: "Gender",
    healthNotes: "Health notes",
    goals: "Goals",
    save: "Save",
    saving: "Saving...",
    saved: "Saved",
    error: "Save failed",
    dailyKcalGoal: "kcal/day",
    dailyProteinGGoal: "Protein (g/day)",
    dailyCarbsGGoal: "Carbs (g/day)",
    dailyFatGGoal: "Fat (g/day)",
    logoutHeading: "Account",
    loadError: "Could not load your profile.",
    retry: "Retry",
    notifications: {
      title: "Notifications",
      description: "Push reminders",
      statusGranted: "Active on this device",
      statusDefault: "Permission needed",
      statusDenied: "Permission denied",
      statusUnsupported: "Not supported on this browser",
      disable: "Disable",
      testButton: "Send test notification",
    },
  },
  push: {
    test: {
      success: "Test notification sent (recipients: {count})",
      zero: "No subscriptions yet",
      error: "Failed to send test notification",
    },
  },
  errors: {
    api: {
      generic: "Something went wrong. Please try again.",
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

const userId = "ffffffff-aaaa-aaaa-aaaa-aaaaaaaaaaaa";

function userMe(overrides: Record<string, unknown> = {}) {
  return {
    id: userId,
    email: "fatih@test.local",
    displayName: "Fatih",
    role: "USER",
    profile: {
      heightCm: 180,
      weightKg: 78,
      birthDate: "1990-05-01",
      gender: "male",
      healthNotes: null,
      goals: null,
      dailyKcalGoal: null,
      dailyProteinGGoal: null,
      dailyCarbsGGoal: null,
      dailyFatGGoal: null,
    },
    ...overrides,
  };
}

describe("ProfileClient", () => {
  beforeEach(() => {
    clearTokens();
    window.localStorage.clear();
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  it("renders the form populated with the current profile", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async (input: string | URL | Request) => {
        const url = typeof input === "string" ? input : input.toString();
        if (url.endsWith("/api/users/me")) return jsonResponse(200, userMe());
        if (url.endsWith("/api/supplements")) return jsonResponse(200, []);
        if (url.includes("/api/users/me/webhook-tokens"))
          return jsonResponse(200, []);
        throw new Error("unexpected fetch: " + url);
      })
    );

    renderClient(<ProfileClient />);

    await waitFor(() =>
      expect(screen.getByLabelText(/display name/i)).toHaveValue("Fatih")
    );
    expect(screen.getByLabelText(/height/i)).toHaveValue(180);
    expect(screen.getByLabelText(/weight/i)).toHaveValue(78);
    expect(screen.getByLabelText(/birth date/i)).toHaveValue("1990-05-01");
    expect(screen.getByText("fatih@test.local")).toBeInTheDocument();
  });

  it("posts updated values and shows the saved flash", async () => {
    let putBody: unknown = null;
    vi.stubGlobal(
      "fetch",
      vi.fn(async (input: string | URL | Request, init?: RequestInit) => {
        const url = typeof input === "string" ? input : input.toString();
        const method = init?.method ?? "GET";
        if (url.endsWith("/api/users/me") && method === "GET") {
          return jsonResponse(200, userMe());
        }
        if (url.endsWith("/api/users/me") && method === "PUT") {
          putBody = JSON.parse(init!.body as string);
          return jsonResponse(
            200,
            userMe({ displayName: "Fatih A", profile: { ...userMe().profile, heightCm: 182 } })
          );
        }
        if (url.endsWith("/api/supplements") && method === "GET") {
          return jsonResponse(200, []);
        }
        if (url.includes("/api/users/me/webhook-tokens") && method === "GET") {
          return jsonResponse(200, []);
        }
        throw new Error("unexpected fetch: " + url + " " + method);
      })
    );

    renderClient(<ProfileClient />);
    await waitFor(() =>
      expect(screen.getByLabelText(/display name/i)).toHaveValue("Fatih")
    );

    const user = userEvent.setup();
    const nameInput = screen.getByLabelText(/display name/i);
    await user.clear(nameInput);
    await user.type(nameInput, "Fatih A");

    await user.click(screen.getByRole("button", { name: "Save" }));

    await waitFor(() => expect(putBody).not.toBeNull());
    expect(putBody).toMatchObject({ displayName: "Fatih A" });
    await screen.findByText("Saved");
  });

  it("shows an error flash when the save request fails", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async (input: string | URL | Request, init?: RequestInit) => {
        const url = typeof input === "string" ? input : input.toString();
        const method = init?.method ?? "GET";
        if (url.endsWith("/api/users/me") && method === "GET") {
          return jsonResponse(200, userMe());
        }
        if (url.endsWith("/api/users/me") && method === "PUT") {
          return jsonResponse(400, {
            timestamp: "2026-04-23T00:00:00Z",
            status: 400,
            error: "bad_request",
            message: "bad",
            path: "/api/users/me",
          });
        }
        if (url.endsWith("/api/supplements") && method === "GET") {
          return jsonResponse(200, []);
        }
        if (url.includes("/api/users/me/webhook-tokens") && method === "GET") {
          return jsonResponse(200, []);
        }
        throw new Error("unexpected fetch: " + url + " " + method);
      })
    );

    renderClient(<ProfileClient />);
    await waitFor(() =>
      expect(screen.getByLabelText(/display name/i)).toHaveValue("Fatih")
    );

    const user = userEvent.setup();
    await user.click(screen.getByRole("button", { name: "Save" }));

    await screen.findByRole("alert");
    expect(screen.getByRole("alert")).toHaveTextContent(
      "Something went wrong. Please try again."
    );
  });

  it("renders accessible names for every form input and the form region", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async (input: string | URL | Request) => {
        const url = typeof input === "string" ? input : input.toString();
        if (url.endsWith("/api/users/me")) return jsonResponse(200, userMe());
        if (url.endsWith("/api/supplements")) return jsonResponse(200, []);
        if (url.includes("/api/users/me/webhook-tokens"))
          return jsonResponse(200, []);
        throw new Error("unexpected fetch: " + url);
      })
    );

    renderClient(<ProfileClient />);

    await waitFor(() =>
      expect(screen.getByLabelText(/display name/i)).toHaveValue("Fatih")
    );

    const form = screen.getByRole("form", { name: /profile/i });
    expect(form).toBeInTheDocument();

    const inputs = within(form).getAllByRole("textbox");
    for (const inp of inputs) {
      expect(inp).toHaveAccessibleName();
    }
    const numberInputs = within(form).getAllByRole("spinbutton");
    for (const inp of numberInputs) {
      expect(inp).toHaveAccessibleName();
    }
  });

  it("shows an inline error banner with retry when fetching the profile fails", async () => {
    let attempt = 0;
    vi.stubGlobal(
      "fetch",
      vi.fn(async (input: string | URL | Request) => {
        const url = typeof input === "string" ? input : input.toString();
        if (url.endsWith("/api/supplements")) return jsonResponse(200, []);
        if (url.includes("/api/users/me/webhook-tokens"))
          return jsonResponse(200, []);
        attempt += 1;
        if (attempt === 1) {
          return jsonResponse(500, {
            timestamp: "2026-05-09T00:00:00Z",
            status: 500,
            error: "internal",
            message: "boom",
            path: "/api/users/me",
          });
        }
        return jsonResponse(200, userMe());
      })
    );

    renderClient(<ProfileClient />);

    await screen.findByRole("alert");
    expect(screen.getByRole("alert")).toHaveTextContent(
      "Could not load your profile."
    );

    const user = userEvent.setup();
    await user.click(screen.getByRole("button", { name: "Retry" }));

    await waitFor(() =>
      expect(screen.getByLabelText(/display name/i)).toHaveValue("Fatih")
    );
  });

  it("mounts the notifications section", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async (input: string | URL | Request) => {
        const url = typeof input === "string" ? input : input.toString();
        if (url.endsWith("/api/users/me")) return jsonResponse(200, userMe());
        if (url.endsWith("/api/supplements")) return jsonResponse(200, []);
        if (url.includes("/api/users/me/webhook-tokens"))
          return jsonResponse(200, []);
        throw new Error("unexpected fetch: " + url);
      })
    );

    renderClient(<ProfileClient />);

    await waitFor(() => {
      expect(screen.getByTestId("notifications-section")).toBeInTheDocument();
    });
  });

  it("uses the typed-error helper for the save failure flash", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async (input: string | URL | Request, init?: RequestInit) => {
        const url = typeof input === "string" ? input : input.toString();
        const method = init?.method ?? "GET";
        if (url.endsWith("/api/users/me") && method === "GET") {
          return jsonResponse(200, userMe());
        }
        if (url.endsWith("/api/users/me") && method === "PUT") {
          return jsonResponse(400, {
            timestamp: "2026-05-09T00:00:00Z",
            status: 400,
            error: "bad_request",
            message: "rejected",
            code: "FUTURE_CODE",
            path: "/api/users/me",
          });
        }
        if (url.endsWith("/api/supplements") && method === "GET") {
          return jsonResponse(200, []);
        }
        if (url.includes("/api/users/me/webhook-tokens") && method === "GET") {
          return jsonResponse(200, []);
        }
        throw new Error("unexpected fetch: " + url + " " + method);
      })
    );

    renderClient(<ProfileClient />);
    await waitFor(() =>
      expect(screen.getByLabelText(/display name/i)).toHaveValue("Fatih")
    );

    const user = userEvent.setup();
    await user.click(screen.getByRole("button", { name: "Save" }));

    await screen.findByRole("alert");
    expect(screen.getByRole("alert")).toHaveTextContent(
      "Something went wrong. Please try again."
    );
  });
});
