import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { NextIntlClientProvider } from "next-intl";
import type { ReactElement } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: vi.fn(), replace: vi.fn(), refresh: vi.fn() }),
  useSearchParams: () => new URLSearchParams(),
  usePathname: () => "/nutrition",
}));

import { NutritionClient } from "@/app/(app)/nutrition/nutrition-client";
import { clearTokens } from "@/lib/auth/token-store";

const messages = {
  nutrition: {
    title: "Nutrition",
    prevDay: "Prev",
    nextDay: "Next",
    dayTotals: "Today: {kcal} kcal - {protein}g protein, {carbs}g carbs, {fat}g fat",
    goalsLine: "Goal: {kcal} kcal ({kcalLeft} left) - {protein}p / {carbs}c / {fat}f",
    addTitle: "Log a food",
    searchLabel: "Food",
    searchPlaceholder: "Search foods...",
    selectedHint: "Selected: {name}",
    servingLabel: "Serving (g)",
    add: "Add",
    saving: "Saving...",
    dayList: "Today's foods",
    empty: "No entries for this day yet.",
    delete: "Delete",
    deleteConfirm: "Delete this entry?",
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

const ME = {
  id: "ffffffff-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
  email: "u@test.local",
  displayName: "U",
  role: "USER",
  profile: {
    heightCm: null,
    weightKg: null,
    birthDate: null,
    gender: null,
    healthNotes: null,
    goals: null,
    dailyKcalGoal: 2200,
    dailyProteinGGoal: 160,
    dailyCarbsGGoal: 220,
    dailyFatGGoal: 70,
  },
};

const FOOD = {
  id: "ffffffff-1111-1111-1111-111111111111",
  nameTr: "Muz",
  nameEn: "Banana",
  kcalPer100g: 89,
  proteinG: 1.1,
  carbsG: 23,
  fatG: 0.3,
  defaultServingG: 120,
};

function entry(id: string) {
  return {
    id,
    foodId: FOOD.id,
    foodNameTr: "Muz",
    foodNameEn: "Banana",
    servingG: 200,
    kcal: 178,
    proteinG: 2.2,
    carbsG: 46,
    fatG: 0.6,
    consumedAt: "2026-04-23T10:00:00Z",
    notes: null,
  };
}

describe("NutritionClient", () => {
  beforeEach(() => {
    clearTokens();
    window.localStorage.clear();
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  it("posts a new entry through the autocomplete + serving form", async () => {
    let posted: unknown = null;
    let getCount = 0;
    vi.stubGlobal(
      "fetch",
      vi.fn(async (input: string | URL | Request, init?: RequestInit) => {
        const url = typeof input === "string" ? input : input.toString();
        const method = init?.method ?? "GET";
        if (url.includes("/api/nutrition") && method === "GET") {
          getCount++;
          if (getCount === 1) return jsonResponse(200, []);
          return jsonResponse(200, [entry("ffffffff-2222-2222-2222-222222222222")]);
        }
        if (url.includes("/api/foods") && method === "GET") {
          return jsonResponse(200, [FOOD]);
        }
        if (url.endsWith("/api/nutrition") && method === "POST") {
          posted = JSON.parse(init!.body as string);
          return jsonResponse(201, entry("ffffffff-2222-2222-2222-222222222222"));
        }
        if (url.endsWith("/api/users/me")) return jsonResponse(200, ME);
        throw new Error("unexpected fetch: " + url + " " + method);
      })
    );

    renderClient(<NutritionClient />);
    await screen.findByText("No entries for this day yet.");

    const user = userEvent.setup();
    fireEvent.change(screen.getByLabelText("Food"), {
      target: { value: "muz" },
    });
    const suggestion = await screen.findByRole("button", { name: /Banana/ });
    await user.click(suggestion);

    fireEvent.change(screen.getByLabelText("Serving (g)"), {
      target: { value: "200" },
    });
    await user.click(screen.getByRole("button", { name: "Add" }));

    await waitFor(() => expect(posted).not.toBeNull());
    expect(posted).toMatchObject({
      foodId: FOOD.id,
      servingG: 200,
    });
    await waitFor(() =>
      expect(screen.getByText(/Banana/)).toBeInTheDocument()
    );
  });

  it("deletes an entry when confirmed", async () => {
    let deletedId: string | null = null;
    let getCount = 0;
    vi.stubGlobal(
      "fetch",
      vi.fn(async (input: string | URL | Request, init?: RequestInit) => {
        const url = typeof input === "string" ? input : input.toString();
        const method = init?.method ?? "GET";
        if (url.includes("/api/nutrition") && method === "GET") {
          getCount++;
          if (getCount === 1)
            return jsonResponse(200, [
              entry("ffffffff-3333-3333-3333-333333333333"),
            ]);
          return jsonResponse(200, []);
        }
        if (url.includes("/api/nutrition/") && method === "DELETE") {
          deletedId = url.split("/api/nutrition/")[1] ?? null;
          return new Response(null, { status: 204 });
        }
        if (url.endsWith("/api/users/me")) return jsonResponse(200, ME);
        throw new Error("unexpected fetch: " + url + " " + method);
      })
    );
    vi.stubGlobal("confirm", vi.fn(() => true));

    renderClient(<NutritionClient />);
    await screen.findByText(/Banana/);

    const user = userEvent.setup();
    await user.click(
      screen.getByTestId("delete-ffffffff-3333-3333-3333-333333333333")
    );

    await waitFor(() =>
      expect(deletedId).toBe("ffffffff-3333-3333-3333-333333333333")
    );
  });

  it("navigates to the previous day when day-prev is clicked", async () => {
    const seenDates: string[] = [];
    vi.stubGlobal(
      "fetch",
      vi.fn(async (input: string | URL | Request) => {
        const url = typeof input === "string" ? input : input.toString();
        if (url.endsWith("/api/users/me")) return jsonResponse(200, ME);
        const m = url.match(/date=([^&]+)/);
        if (m) seenDates.push(decodeURIComponent(m[1]));
        return jsonResponse(200, []);
      })
    );

    renderClient(<NutritionClient />);
    await screen.findByText("No entries for this day yet.");

    const user = userEvent.setup();
    await user.click(screen.getByTestId("day-prev"));

    await waitFor(() => expect(seenDates.length).toBeGreaterThanOrEqual(2));
    expect(seenDates[0]).not.toEqual(seenDates[seenDates.length - 1]);
  });
});
