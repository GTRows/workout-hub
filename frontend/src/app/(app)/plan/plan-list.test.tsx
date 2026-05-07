import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { NextIntlClientProvider } from "next-intl";
import type { ReactElement } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: vi.fn(), replace: vi.fn(), refresh: vi.fn() }),
  useSearchParams: () => new URLSearchParams(),
  usePathname: () => "/plan",
}));

import { PlanList } from "@/app/(app)/plan/plan-list";
import { clearTokens } from "@/lib/auth/token-store";

const messages = {
  plan: {
    title: "Weekly plan",
    loading: "Loading...",
    noPlan: "No active plan",
    emptyDay: "No exercises",
    moveUp: "Move up",
    moveDown: "Move down",
    weekday: {
      "1": "Monday",
      "2": "Tuesday",
      "3": "Wednesday",
      "4": "Thursday",
      "5": "Friday",
      "6": "Saturday",
      "7": "Sunday",
    },
    setsReps: "{sets} sets x {min}-{max} reps",
    setsOnly: "{sets} sets",
    manageTitle: "My plans",
    newPlanTitle: "New plan",
    newPlanNameLabel: "Plan name",
    newPlanSubmit: "Create",
    activeBadge: "Active",
    activate: "Set active",
    rename: "Rename",
    delete: "Delete",
    deleteConfirm: "Delete this plan?",
    cannotDeleteActive: "Cannot delete the active plan",
    noPlans: "No plans yet",
    renameDialogTitle: "Rename plan",
    dialogSave: "Save",
    dialogCancel: "Cancel",
    nameRequired: "Plan name is required",
    mutationError: "Operation failed. Try again.",
  },
};

function renderList(ui: ReactElement) {
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

function noContent(): Response {
  return new Response(null, { status: 204 });
}

const planA = {
  id: "ffffffff-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
  name: "Plan A",
  active: true,
  createdAt: "2026-04-01T00:00:00Z",
  updatedAt: "2026-04-01T00:00:00Z",
  days: [],
};

const planB = {
  id: "ffffffff-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
  name: "Plan B",
  active: false,
  createdAt: "2026-04-02T00:00:00Z",
  updatedAt: "2026-04-02T00:00:00Z",
  days: [],
};

describe("PlanList", () => {
  beforeEach(() => {
    clearTokens();
    window.localStorage.clear();
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  it("renders empty state when no plans exist", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async (input: string | URL | Request) => {
        const url = typeof input === "string" ? input : input.toString();
        if (url.endsWith("/api/workout-plans")) {
          return jsonResponse(200, []);
        }
        throw new Error("unexpected fetch: " + url);
      })
    );
    renderList(<PlanList />);
    expect(await screen.findByText("No plans yet")).toBeInTheDocument();
  });

  it("renders the list with active badge on the active plan", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async (input: string | URL | Request) => {
        const url = typeof input === "string" ? input : input.toString();
        if (url.endsWith("/api/workout-plans")) {
          return jsonResponse(200, [planA, planB]);
        }
        throw new Error("unexpected fetch: " + url);
      })
    );
    renderList(<PlanList />);

    await screen.findByText("Plan A");
    expect(screen.getByText("Plan B")).toBeInTheDocument();

    const rowA = screen.getByTestId(`plan-row-${planA.id}`);
    expect(within(rowA).getByText("Active")).toBeInTheDocument();

    const rowB = screen.getByTestId(`plan-row-${planB.id}`);
    expect(within(rowB).queryByText("Active")).toBeNull();
  });

  it("submits the create form", async () => {
    let postedBody: unknown = null;
    vi.stubGlobal(
      "fetch",
      vi.fn(async (input: string | URL | Request, init?: RequestInit) => {
        const url = typeof input === "string" ? input : input.toString();
        const method = init?.method ?? "GET";
        if (url.endsWith("/api/workout-plans") && method === "GET") {
          return jsonResponse(200, []);
        }
        if (url.endsWith("/api/workout-plans") && method === "POST") {
          postedBody = JSON.parse(init!.body as string);
          return jsonResponse(201, {
            id: "ffffffff-1111-1111-1111-111111111111",
            name: "PPL",
            active: false,
            createdAt: "2026-04-03T00:00:00Z",
            updatedAt: "2026-04-03T00:00:00Z",
            days: [],
          });
        }
        throw new Error("unexpected fetch: " + url + " " + method);
      })
    );

    renderList(<PlanList />);
    await screen.findByText("No plans yet");

    const user = userEvent.setup();
    const input = screen.getByLabelText("Plan name");
    await user.type(input, "PPL");
    await user.click(screen.getByRole("button", { name: "Create" }));

    await waitFor(() => expect(postedBody).not.toBeNull());
    expect(postedBody).toEqual({ name: "PPL" });
  });

  it("activates a plan", async () => {
    let activateUrl: string | null = null;
    vi.stubGlobal(
      "fetch",
      vi.fn(async (input: string | URL | Request, init?: RequestInit) => {
        const url = typeof input === "string" ? input : input.toString();
        const method = init?.method ?? "GET";
        if (url.endsWith("/api/workout-plans") && method === "GET") {
          return jsonResponse(200, [planA, planB]);
        }
        if (url.includes("/activate") && method === "POST") {
          activateUrl = url;
          return jsonResponse(200, { ...planB, active: true });
        }
        throw new Error("unexpected fetch: " + url + " " + method);
      })
    );

    renderList(<PlanList />);
    await screen.findByText("Plan B");

    const user = userEvent.setup();
    await user.click(screen.getByTestId(`activate-${planB.id}`));

    await waitFor(() => expect(activateUrl).not.toBeNull());
    expect(activateUrl).toContain(`/api/workout-plans/${planB.id}/activate`);
  });

  it("deletes a non-active plan when confirm returns true", async () => {
    let deleteUrl: string | null = null;
    vi.stubGlobal(
      "fetch",
      vi.fn(async (input: string | URL | Request, init?: RequestInit) => {
        const url = typeof input === "string" ? input : input.toString();
        const method = init?.method ?? "GET";
        if (url.endsWith("/api/workout-plans") && method === "GET") {
          return jsonResponse(200, [planA, planB]);
        }
        if (url.includes(planB.id) && method === "DELETE") {
          deleteUrl = url;
          return noContent();
        }
        throw new Error("unexpected fetch: " + url + " " + method);
      })
    );

    vi.spyOn(window, "confirm").mockReturnValue(true);

    renderList(<PlanList />);
    await screen.findByText("Plan B");

    const user = userEvent.setup();
    await user.click(screen.getByTestId(`delete-${planB.id}`));

    await waitFor(() => expect(deleteUrl).not.toBeNull());
    expect(deleteUrl).toContain(`/api/workout-plans/${planB.id}`);
  });

  it("disables delete on the active plan", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async (input: string | URL | Request) => {
        const url = typeof input === "string" ? input : input.toString();
        if (url.endsWith("/api/workout-plans")) {
          return jsonResponse(200, [planA]);
        }
        throw new Error("unexpected fetch: " + url);
      })
    );

    renderList(<PlanList />);
    await screen.findByText("Plan A");

    const deleteBtn = screen.getByTestId(`delete-${planA.id}`);
    expect(deleteBtn).toBeDisabled();
  });
});
