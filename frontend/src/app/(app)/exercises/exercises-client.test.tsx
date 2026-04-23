import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { NextIntlClientProvider } from "next-intl";
import type { ReactElement } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: vi.fn(), replace: vi.fn(), refresh: vi.fn() }),
  useSearchParams: () => new URLSearchParams(),
  usePathname: () => "/exercises",
}));

import { ExercisesClient } from "@/app/(app)/exercises/exercises-client";
import { clearTokens } from "@/lib/auth/token-store";

const messages = {
  exercises: {
    title: "Exercise catalog",
    searchPlaceholder: "Search exercises...",
    filterCategoryLabel: "Category",
    filterEquipmentLabel: "Equipment",
    filterDifficultyLabel: "Difficulty",
    filterAny: "Any",
    category: {
      push: "Push",
      pull: "Pull",
      legs: "Legs",
      cardio: "Cardio",
      core: "Core",
      forearm: "Forearm",
    },
    equipment: {
      bodyweight: "Bodyweight",
      dumbbell: "Dumbbell",
      zbar: "Z-bar",
      bar: "Bar",
      wrist_tool: "Wrist tool",
      machine: "Machine",
      other: "Other",
    },
    difficulty: {
      beginner: "Beginner",
      intermediate: "Intermediate",
      advanced: "Advanced",
    },
    loading: "Loading...",
    empty: "No exercises match.",
    notFound: "Exercise not found",
    formTips: "Form tips",
    commonMistakes: "Common mistakes",
    noDescription: "No description yet.",
    backToCatalog: "Back to catalog",
    musclePrimary: "Primary",
    muscleSecondary: "Secondary",
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

function pagePayload(items: Array<{ id: string; nameTr: string; nameEn: string; category: string; equipment: string; difficulty: string }>) {
  return {
    content: items.map((i) => ({
      id: i.id,
      nameTr: i.nameTr,
      nameEn: i.nameEn,
      category: i.category,
      equipment: i.equipment,
      musclePrimary: "chest",
      muscleSecondary: null,
      descriptionTr: null,
      descriptionEn: null,
      formTipsTr: [],
      formTipsEn: [],
      commonMistakesTr: [],
      commonMistakesEn: [],
      imageUrl: null,
      videoUrl: null,
      difficulty: i.difficulty,
    })),
    totalElements: items.length,
    totalPages: 1,
    number: 0,
    size: 50,
  };
}

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "content-type": "application/json" },
  });
}

describe("ExercisesClient", () => {
  beforeEach(() => {
    clearTokens();
    window.localStorage.clear();
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("lists exercises returned by the unfiltered query", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async (input: string | URL | Request) => {
        const url = typeof input === "string" ? input : input.toString();
        if (url.includes("/api/exercises") && !url.includes("/search")) {
          return jsonResponse(
            200,
            pagePayload([
              {
                id: "ffffffff-1111-1111-1111-111111111111",
                nameTr: "Sinav",
                nameEn: "Push-up",
                category: "push",
                equipment: "bodyweight",
                difficulty: "beginner",
              },
              {
                id: "ffffffff-2222-2222-2222-222222222222",
                nameTr: "Goblet Squat",
                nameEn: "Goblet Squat",
                category: "legs",
                equipment: "dumbbell",
                difficulty: "beginner",
              },
            ])
          );
        }
        throw new Error("unexpected fetch: " + url);
      })
    );

    renderClient(<ExercisesClient />);
    // Locale is 'en' in this test so pickLocaleField returns nameEn.
    expect(await screen.findByText("Push-up")).toBeInTheDocument();
    expect(screen.getByText("Goblet Squat")).toBeInTheDocument();
  });

  it("passes selected category filter into the backend query", async () => {
    let lastUrl = "";
    vi.stubGlobal(
      "fetch",
      vi.fn(async (input: string | URL | Request) => {
        const url = typeof input === "string" ? input : input.toString();
        lastUrl = url;
        if (url.includes("category=push")) {
          return jsonResponse(
            200,
            pagePayload([
              {
                id: "ffffffff-3333-3333-3333-333333333333",
                nameTr: "Sinav",
                nameEn: "Push-up",
                category: "push",
                equipment: "bodyweight",
                difficulty: "beginner",
              },
            ])
          );
        }
        return jsonResponse(200, pagePayload([]));
      })
    );

    renderClient(<ExercisesClient />);
    const user = userEvent.setup();
    await user.selectOptions(screen.getByLabelText("Category"), "push");

    await waitFor(() => expect(lastUrl).toContain("category=push"));
    expect(await screen.findByText("Push-up")).toBeInTheDocument();
  });

  it("switches to search endpoint when the query input has text", async () => {
    const calls: string[] = [];
    vi.stubGlobal(
      "fetch",
      vi.fn(async (input: string | URL | Request) => {
        const url = typeof input === "string" ? input : input.toString();
        calls.push(url);
        if (url.includes("/api/exercises/search")) {
          return jsonResponse(
            200,
            pagePayload([
              {
                id: "ffffffff-4444-4444-4444-444444444444",
                nameTr: "Plank",
                nameEn: "Plank",
                category: "core",
                equipment: "bodyweight",
                difficulty: "beginner",
              },
            ])
          );
        }
        return jsonResponse(200, pagePayload([]));
      })
    );

    renderClient(<ExercisesClient />);
    const user = userEvent.setup();
    await user.type(screen.getByRole("searchbox"), "plank");

    await waitFor(
      () => expect(calls.some((u) => u.includes("/api/exercises/search?q=plank"))).toBe(true),
      { timeout: 2000 }
    );
    expect(await screen.findByText("Plank")).toBeInTheDocument();
  });
});
