import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { NextIntlClientProvider } from "next-intl";
import type { ReactElement } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: vi.fn(), replace: vi.fn(), refresh: vi.fn() }),
  useSearchParams: () => new URLSearchParams(),
  usePathname: () => "/profile",
}));

import { WebhookTokensSection } from "@/app/(app)/profile/webhook-tokens-section";
import { clearTokens } from "@/lib/auth/token-store";

const messages = {
  webhookTokens: {
    title: "Smart-scale webhook",
    description: "desc",
    mint: "Generate URL",
    minting: "Generating...",
    revoke: "Revoke",
    revokeConfirm: "Revoke this webhook URL?",
    lastUsed: "Last ping: {time}",
    never: "Never used",
    loadError: "Could not load tokens.",
    toastMintError: "Could not mint token.",
    toastRevokeError: "Could not revoke token.",
  },
  profile: {
    retry: "Retry",
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

const TOKEN = {
  id: "ffffffff-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
  token: "abcDEF123",
  purpose: "scale",
  createdAt: "2026-04-25T08:00:00Z",
  lastUsedAt: null,
};

describe("WebhookTokensSection", () => {
  beforeEach(() => {
    clearTokens();
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  it("renders existing tokens with the full webhook URL", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async (input: string | URL | Request) => {
        const url = typeof input === "string" ? input : input.toString();
        if (url.includes("/api/users/me/webhook-tokens")) {
          return jsonResponse(200, [TOKEN]);
        }
        throw new Error("unexpected fetch: " + url);
      })
    );
    renderClient(<WebhookTokensSection />);

    await waitFor(() =>
      expect(screen.getByTestId(`token-url-${TOKEN.id}`)).toBeInTheDocument()
    );
    expect(screen.getByTestId(`token-url-${TOKEN.id}`).textContent).toContain(
      `/api/webhooks/scale/${TOKEN.token}`
    );
  });

  it("posts to mint a new token when the button is clicked", async () => {
    let mintCalled = false;
    let getCount = 0;
    vi.stubGlobal(
      "fetch",
      vi.fn(async (input: string | URL | Request, init?: RequestInit) => {
        const url = typeof input === "string" ? input : input.toString();
        const method = init?.method ?? "GET";
        if (url.includes("/api/users/me/webhook-tokens") && method === "POST") {
          mintCalled = true;
          return jsonResponse(201, TOKEN);
        }
        if (url.includes("/api/users/me/webhook-tokens")) {
          getCount++;
          if (getCount === 1) return jsonResponse(200, []);
          return jsonResponse(200, [TOKEN]);
        }
        throw new Error("unexpected fetch: " + url + " " + method);
      })
    );
    renderClient(<WebhookTokensSection />);
    const user = userEvent.setup();
    await user.click(screen.getByTestId("mint-token"));
    await waitFor(() => expect(mintCalled).toBe(true));
  });

  it("shows an inline error banner with retry when webhook tokens fail to load", async () => {
    let attempt = 0;
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => {
        attempt += 1;
        if (attempt === 1) {
          return jsonResponse(500, {
            timestamp: "2026-05-09T00:00:00Z",
            status: 500,
            error: "internal",
            message: "boom",
            path: "/api/users/me/webhook-tokens",
          });
        }
        return jsonResponse(200, []);
      })
    );

    renderClient(<WebhookTokensSection />);

    await screen.findByRole("alert");
    expect(screen.getByRole("alert")).toHaveTextContent(
      "Could not load tokens."
    );

    const user = userEvent.setup();
    await user.click(screen.getByRole("button", { name: "Retry" }));

    await waitFor(() => expect(screen.queryByRole("alert")).toBeNull());
  });

  it("shows the mint-error toast when minting fails", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async (input: string | URL | Request, init?: RequestInit) => {
        const url = typeof input === "string" ? input : input.toString();
        const method = init?.method ?? "GET";
        if (url.includes("/api/users/me/webhook-tokens") && method === "GET") {
          return jsonResponse(200, []);
        }
        if (url.includes("/api/users/me/webhook-tokens") && method === "POST") {
          return jsonResponse(500, {
            timestamp: "2026-05-09T00:00:00Z",
            status: 500,
            error: "internal",
            message: "db busy",
            path: "/api/users/me/webhook-tokens",
          });
        }
        throw new Error("unexpected fetch: " + url + " " + method);
      })
    );

    renderClient(<WebhookTokensSection />);
    const user = userEvent.setup();
    await user.click(screen.getByTestId("mint-token"));

    const toast = await screen.findByTestId("pr-toast");
    expect(toast).toHaveTextContent("Could not mint token.");
  });
});
