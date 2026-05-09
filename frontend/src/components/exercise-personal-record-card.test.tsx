import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import { NextIntlClientProvider } from "next-intl";
import type { ReactElement } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: vi.fn(), replace: vi.fn(), refresh: vi.fn() }),
  useSearchParams: () => new URLSearchParams(),
  usePathname: () => "/exercises/x",
}));

import { ExercisePersonalRecordCard } from "@/components/exercise-personal-record-card";
import { clearTokens } from "@/lib/auth/token-store";

const messages = {
  exercises: {
    loading: "Loading...",
    personalRecordTitle: "Personal record",
    personalRecordValue: "{weight} kg x {reps}",
    personalRecordAchievedDaysAgo: "{days} days ago",
    personalRecordAchievedToday: "Today",
    personalRecordAchievedAbsolute: "{date}",
    personalRecordEmpty: "No PR yet",
    personalRecordEmptyCta: "Log a set",
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

const EXERCISE_ID = "ffffffff-1111-1111-1111-111111111111";

type StubSet = {
  id: string;
  exerciseId: string | null;
  exerciseNameTr: string | null;
  exerciseNameEn: string | null;
  setNumber: number;
  repsDone: number;
  weightKg: number | null;
  rpe: number | null;
  completed: boolean;
  notes: string | null;
  newPr: boolean | null;
};

function stubSet(overrides: Partial<StubSet> = {}): StubSet {
  return {
    id: "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
    exerciseId: EXERCISE_ID,
    exerciseNameTr: "Bench",
    exerciseNameEn: "Bench",
    setNumber: 1,
    repsDone: 5,
    weightKg: 80,
    rpe: null,
    completed: true,
    notes: null,
    newPr: true,
    ...overrides,
  };
}

function isoDaysAgo(days: number): string {
  const d = new Date();
  d.setDate(d.getDate() - days);
  return d.toISOString();
}

describe("ExercisePersonalRecordCard", () => {
  beforeEach(() => {
    clearTokens();
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  it("renders weight x reps when a newPr-flagged set is present", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async () =>
        jsonResponse(200, {
          sessionId: "11111111-1111-1111-1111-111111111111",
          startedAt: isoDaysAgo(0),
          endedAt: null,
          sets: [stubSet({ weightKg: 80, repsDone: 5, newPr: true })],
        })
      )
    );

    renderClient(<ExercisePersonalRecordCard exerciseId={EXERCISE_ID} />);

    await waitFor(() =>
      expect(screen.getByText(/80 kg x 5/)).toBeInTheDocument()
    );
    expect(screen.getByText("Today")).toBeInTheDocument();
  });

  it("renders the relative-day line when the achievement is within 7 days", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async () =>
        jsonResponse(200, {
          sessionId: "11111111-1111-1111-1111-111111111111",
          startedAt: isoDaysAgo(3),
          endedAt: null,
          sets: [stubSet({ newPr: true })],
        })
      )
    );

    renderClient(<ExercisePersonalRecordCard exerciseId={EXERCISE_ID} />);

    await waitFor(() =>
      expect(screen.getByText(/3 days ago/)).toBeInTheDocument()
    );
  });

  it("renders an absolute-date line when the achievement is older than 7 days", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async () =>
        jsonResponse(200, {
          sessionId: "11111111-1111-1111-1111-111111111111",
          startedAt: isoDaysAgo(30),
          endedAt: null,
          sets: [stubSet({ newPr: true })],
        })
      )
    );

    const { container } = renderClient(
      <ExercisePersonalRecordCard exerciseId={EXERCISE_ID} />
    );

    await waitFor(() =>
      expect(screen.getByText(/80 kg x 5/)).toBeInTheDocument()
    );
    // The absolute branch renders neither "Today" nor "X days ago".
    expect(screen.queryByText("Today")).toBeNull();
    expect(container.textContent ?? "").not.toMatch(/days ago/);
  });

  it("renders the empty CTA when the endpoint returns 204", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => new Response(null, { status: 204 }))
    );

    renderClient(<ExercisePersonalRecordCard exerciseId={EXERCISE_ID} />);

    await waitFor(() =>
      expect(screen.getByText("No PR yet")).toBeInTheDocument()
    );
    expect(screen.getByTestId("exercise-pr-empty-cta")).toBeInTheDocument();
  });

  it("renders the empty CTA when no set carries newPr=true", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async () =>
        jsonResponse(200, {
          sessionId: "11111111-1111-1111-1111-111111111111",
          startedAt: isoDaysAgo(2),
          endedAt: null,
          sets: [stubSet({ newPr: false }), stubSet({ newPr: null, id: "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb" })],
        })
      )
    );

    renderClient(<ExercisePersonalRecordCard exerciseId={EXERCISE_ID} />);

    await waitFor(() =>
      expect(screen.getByText("No PR yet")).toBeInTheDocument()
    );
  });
});
