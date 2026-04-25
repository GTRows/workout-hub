import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { NextIntlClientProvider } from "next-intl";
import type { ReactElement } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: vi.fn(), replace: vi.fn(), refresh: vi.fn() }),
  useSearchParams: () => new URLSearchParams(),
  usePathname: () => "/session/test",
}));

import { ExerciseDetailModal } from "@/app/(app)/session/[id]/exercise-detail-modal";
import { clearTokens } from "@/lib/auth/token-store";

const messages = {
  exercises: {
    loading: "Loading...",
    formTips: "Form tips",
    commonMistakes: "Common mistakes",
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
  formTipsTr: ["Bilekleri duz tut", "Kuregi sik"],
  formTipsEn: ["Keep wrists straight", "Squeeze shoulder blades"],
  commonMistakesTr: ["Govdeyi havalandirmak"],
  commonMistakesEn: ["Bouncing the bar"],
  imageUrl: null,
  videoUrl: null,
  difficulty: "intermediate",
};

describe("ExerciseDetailModal", () => {
  beforeEach(() => {
    clearTokens();
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  function stubFetch() {
    vi.stubGlobal(
      "fetch",
      vi.fn(async (input: string | URL | Request) => {
        const url = typeof input === "string" ? input : input.toString();
        if (url.includes(`/api/exercises/${EXERCISE.id}`)) {
          return jsonResponse(200, EXERCISE);
        }
        if (url.includes("/last-performance")) {
          return jsonResponse(200, null);
        }
        throw new Error("unexpected fetch: " + url);
      })
    );
  }

  it("renders bilingual form tips and common mistakes from the loaded exercise", async () => {
    stubFetch();
    const onClose = vi.fn();
    renderClient(
      <ExerciseDetailModal exerciseId={EXERCISE.id} onClose={onClose} />
    );

    await waitFor(() =>
      expect(screen.getByText(/Keep wrists straight/)).toBeInTheDocument()
    );
    expect(screen.getByText(/Bouncing the bar/)).toBeInTheDocument();
    expect(screen.getByTestId("exercise-detail-tips")).toBeInTheDocument();
    expect(screen.getByTestId("exercise-detail-mistakes")).toBeInTheDocument();
  });

  it("calls onClose when the close button is clicked", async () => {
    stubFetch();
    const onClose = vi.fn();
    renderClient(
      <ExerciseDetailModal exerciseId={EXERCISE.id} onClose={onClose} />
    );
    const user = userEvent.setup();
    await user.click(screen.getByTestId("exercise-detail-close"));
    expect(onClose).toHaveBeenCalledTimes(1);
  });

  it("calls onClose when Escape is pressed", async () => {
    stubFetch();
    const onClose = vi.fn();
    renderClient(
      <ExerciseDetailModal exerciseId={EXERCISE.id} onClose={onClose} />
    );
    fireEvent.keyDown(document, { key: "Escape" });
    expect(onClose).toHaveBeenCalledTimes(1);
  });
});
