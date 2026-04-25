import { render, screen } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { ExerciseMedia } from "@/components/exercise-media";

class MockIntersectionObserver implements IntersectionObserver {
  readonly root = null;
  readonly rootMargin = "";
  readonly thresholds: ReadonlyArray<number> = [];
  private callback: IntersectionObserverCallback;

  constructor(cb: IntersectionObserverCallback) {
    this.callback = cb;
  }
  observe(target: Element): void {
    this.callback(
      [
        {
          isIntersecting: true,
          target,
          intersectionRatio: 1,
          time: 0,
          boundingClientRect: target.getBoundingClientRect(),
          intersectionRect: target.getBoundingClientRect(),
          rootBounds: null,
        } as IntersectionObserverEntry,
      ],
      this
    );
  }
  unobserve(): void {}
  disconnect(): void {}
  takeRecords(): IntersectionObserverEntry[] {
    return [];
  }
}

describe("ExerciseMedia", () => {
  beforeEach(() => {
    vi.stubGlobal("IntersectionObserver", MockIntersectionObserver);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("renders nothing when no media URLs are provided", () => {
    const { container } = render(
      <ExerciseMedia videoUrl={null} imageUrl={null} alt="No media" />
    );
    expect(container.firstChild).toBeNull();
  });

  it("renders a video tag with title and aria-label when videoUrl is set", () => {
    render(
      <ExerciseMedia
        videoUrl="https://example.com/squat.mp4"
        imageUrl={null}
        alt="Back squat"
      />
    );
    const v = screen.getByTestId("exercise-media-video");
    expect(v).toHaveAttribute("src", "https://example.com/squat.mp4");
    expect(v).toHaveAttribute("title", "Back squat");
    expect(v).toHaveAttribute("aria-label", "Back squat");
  });

  it("falls back to a lazy <img> when only imageUrl is set", () => {
    render(
      <ExerciseMedia
        videoUrl={null}
        imageUrl="https://example.com/squat.gif"
        alt="Back squat"
      />
    );
    const img = screen.getByTestId("exercise-media-image");
    expect(img).toHaveAttribute("loading", "lazy");
    expect(img).toHaveAttribute("alt", "Back squat");
  });
});
