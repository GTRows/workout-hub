import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import { NextIntlClientProvider } from "next-intl";
import type { ReactElement, ReactNode } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: vi.fn(), replace: vi.fn(), refresh: vi.fn() }),
  useSearchParams: () => new URLSearchParams(),
  usePathname: () => "/exercises/x",
}));

vi.mock("recharts", async () => {
  const actual = await vi.importActual<typeof import("recharts")>("recharts");
  return {
    ...actual,
    ResponsiveContainer: ({ children }: { children: ReactNode }) => (
      <div style={{ width: 600, height: 300 }} data-testid="recharts-container">
        {children}
      </div>
    ),
  };
});

import { ExerciseDetailClient } from "@/app/(app)/exercises/[id]/exercise-detail-client";
import { clearTokens } from "@/lib/auth/token-store";

const messages = {
  exercises: {
    title: "Exercise catalog",
    loading: "Loading...",
    empty: "No exercises match.",
    notFound: "Exercise not found",
    formTips: "Form tips",
    commonMistakes: "Common mistakes",
    noDescription: "No description yet.",
    backToCatalog: "Back to catalog",
    musclePrimary: "Primary",
    muscleSecondary: "Secondary",
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
    personalRecordTitle: "Personal record",
    personalRecordValue: "{weight} kg x {reps}",
    personalRecordAchievedDaysAgo: "{days} days ago",
    personalRecordAchievedToday: "Today",
    personalRecordAchievedAbsolute: "{date}",
    personalRecordEmpty: "No PR yet",
    personalRecordEmptyCta: "Log a set",
    progressChartTitle: "Progress",
    progressChartEmpty: "No progression yet",
    progressChartLoading: "Loading...",
    progressChartTooltipOneRm: "Estimated 1RM: {value} kg",
    progressChartTooltipMaxWeight: "Max weight: {value} kg",
    progressChartTooltipVolume: "Volume: {value} kg",
    progressChartTooltipSets: "Sets: {count}",
    progressChartTooltipReps: "Top reps: {count}",
  },
};

function renderClient(ui: ReactElement): ReturnType<typeof render> {
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

const EXERCISE = {
  id: "ffffffff-1111-1111-1111-111111111111",
  nameTr: "Bench Press",
  nameEn: "Bench Press",
  category: "push",
  equipment: "bar",
  musclePrimary: "chest",
  muscleSecondary: "triceps",
  descriptionTr: "Yatay sinav.",
  descriptionEn: "Horizontal press.",
  formTipsTr: ["Bilekleri duz tut"],
  formTipsEn: ["Keep wrists straight"],
  commonMistakesTr: ["Govdeyi havalandirmak"],
  commonMistakesEn: ["Bouncing the bar"],
  imageUrl: null,
  videoUrl: null,
  difficulty: "intermediate",
};

describe("ExerciseDetailClient", () => {
  beforeEach(() => {
    clearTokens();
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  function stubFetchOk() {
    vi.stubGlobal(
      "fetch",
      vi.fn(async (input: string | URL | Request) => {
        const url = typeof input === "string" ? input : input.toString();
        if (url.includes("/last-performance")) {
          return new Response(null, { status: 204 });
        }
        if (url.includes("/progress")) {
          return jsonResponse(200, []);
        }
        if (url.includes(`/api/exercises/${EXERCISE.id}`)) {
          return jsonResponse(200, EXERCISE);
        }
        throw new Error("unexpected fetch: " + url);
      })
    );
  }

  it("renders the bilingual name, description, tips, and mistakes when the exercise loads", async () => {
    stubFetchOk();
    renderClient(<ExerciseDetailClient id={EXERCISE.id} />);

    await waitFor(() =>
      expect(screen.getByText("Bench Press")).toBeInTheDocument()
    );
    expect(screen.getByText("Horizontal press.")).toBeInTheDocument();
    expect(screen.getByText("Form tips")).toBeInTheDocument();
    expect(screen.getByText("Keep wrists straight")).toBeInTheDocument();
    expect(screen.getByText("Common mistakes")).toBeInTheDocument();
    expect(screen.getByText("Bouncing the bar")).toBeInTheDocument();
  });

  it("mounts the PR card and progress chart between description and form tips", async () => {
    stubFetchOk();
    const { container } = renderClient(
      <ExerciseDetailClient id={EXERCISE.id} />
    );

    await waitFor(() =>
      expect(screen.getByText("Form tips")).toBeInTheDocument()
    );

    const text = container.textContent ?? "";
    const descIdx = text.indexOf("Horizontal press.");
    const prIdx = text.indexOf("Personal record");
    const chartIdx = text.indexOf("Progress");
    const tipsIdx = text.indexOf("Form tips");
    const mistakesIdx = text.indexOf("Common mistakes");

    expect(descIdx).toBeGreaterThanOrEqual(0);
    expect(prIdx).toBeGreaterThan(descIdx);
    expect(chartIdx).toBeGreaterThan(prIdx);
    expect(tipsIdx).toBeGreaterThan(chartIdx);
    expect(mistakesIdx).toBeGreaterThan(tipsIdx);
  });

  it("renders the notFound copy on a 404 from /api/exercises/{id}", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async () =>
        jsonResponse(404, {
          timestamp: "2026-04-01T12:00:00Z",
          status: 404,
          error: "Not Found",
          message: "Exercise not found",
          path: `/api/exercises/${EXERCISE.id}`,
        })
      )
    );

    renderClient(<ExerciseDetailClient id={EXERCISE.id} />);

    await waitFor(() =>
      expect(screen.getByText("Exercise not found")).toBeInTheDocument()
    );
  });
});
