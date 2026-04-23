import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { NextIntlClientProvider } from "next-intl";
import type { ReactElement } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: vi.fn(), replace: vi.fn(), refresh: vi.fn() }),
  useSearchParams: () => new URLSearchParams(),
  usePathname: () => "/profile",
}));

import { SupplementsSection } from "@/app/(app)/profile/supplements-section";
import { clearTokens } from "@/lib/auth/token-store";

const messages = {
  supplements: {
    title: "Supplements",
    description: "List",
    empty: "No supplements yet.",
    name: "Name",
    dosage: "Dosage",
    add: "Add",
    saving: "Saving...",
    delete: "Delete",
    deleteConfirm: "Delete this supplement?",
    timing: {
      label: "Timing",
      morning: "Morning",
      pre_workout: "Pre-workout",
      post_workout: "Post-workout",
      evening: "Evening",
      with_meal: "With meal",
      other: "Other",
    },
  },
};

function renderSection(ui: ReactElement) {
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

function supplement(id: string, name: string) {
  return {
    id,
    name,
    dosage: "5g",
    timing: "morning",
    active: true,
    createdAt: "2026-04-23T00:00:00Z",
    updatedAt: "2026-04-23T00:00:00Z",
  };
}

describe("SupplementsSection", () => {
  beforeEach(() => {
    clearTokens();
    window.localStorage.clear();
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  it("posts a new supplement and refreshes the list", async () => {
    let posted: unknown = null;
    let getCount = 0;
    vi.stubGlobal(
      "fetch",
      vi.fn(async (input: string | URL | Request, init?: RequestInit) => {
        const url = typeof input === "string" ? input : input.toString();
        const method = init?.method ?? "GET";
        if (url.endsWith("/api/supplements") && method === "GET") {
          getCount++;
          if (getCount === 1) return jsonResponse(200, []);
          return jsonResponse(200, [
            supplement("ffffffff-1111-1111-1111-111111111111", "Creatine"),
          ]);
        }
        if (url.endsWith("/api/supplements") && method === "POST") {
          posted = JSON.parse(init!.body as string);
          return jsonResponse(
            201,
            supplement("ffffffff-1111-1111-1111-111111111111", "Creatine")
          );
        }
        throw new Error("unexpected fetch: " + url + " " + method);
      })
    );

    renderSection(<SupplementsSection />);
    await screen.findByText("No supplements yet.");

    fireEvent.change(screen.getByLabelText("Name"), {
      target: { value: "Creatine" },
    });
    fireEvent.change(screen.getByLabelText("Dosage"), {
      target: { value: "5g" },
    });

    const user = userEvent.setup();
    await user.click(screen.getByRole("button", { name: "Add" }));

    await waitFor(() => expect(posted).not.toBeNull());
    expect(posted).toMatchObject({
      name: "Creatine",
      dosage: "5g",
      timing: "morning",
    });

    await waitFor(() =>
      expect(screen.getByText("Creatine")).toBeInTheDocument()
    );
  });

  it("deletes a supplement when confirmed", async () => {
    let deleteCalledWithId: string | null = null;
    let getCount = 0;
    vi.stubGlobal(
      "fetch",
      vi.fn(async (input: string | URL | Request, init?: RequestInit) => {
        const url = typeof input === "string" ? input : input.toString();
        const method = init?.method ?? "GET";
        if (url.endsWith("/api/supplements") && method === "GET") {
          getCount++;
          if (getCount === 1)
            return jsonResponse(200, [
              supplement("ffffffff-2222-2222-2222-222222222222", "Omega-3"),
            ]);
          return jsonResponse(200, []);
        }
        if (url.includes("/api/supplements/") && method === "DELETE") {
          deleteCalledWithId = url.split("/api/supplements/")[1] ?? null;
          return new Response(null, { status: 204 });
        }
        throw new Error("unexpected fetch: " + url + " " + method);
      })
    );

    vi.stubGlobal("confirm", vi.fn(() => true));

    renderSection(<SupplementsSection />);
    await screen.findByText("Omega-3");

    const user = userEvent.setup();
    await user.click(screen.getByRole("button", { name: "Delete" }));

    await waitFor(() =>
      expect(deleteCalledWithId).toBe("ffffffff-2222-2222-2222-222222222222")
    );
  });
});
