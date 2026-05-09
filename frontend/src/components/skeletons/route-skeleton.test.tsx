import { render, screen } from "@testing-library/react";
import { NextIntlClientProvider } from "next-intl";
import { describe, expect, it } from "vitest";
import {
  RouteSkeleton,
  type RouteSkeletonVariant,
} from "./route-skeleton";

const VARIANTS: RouteSkeletonVariant[] = [
  "dashboard",
  "plan",
  "session",
  "history",
  "exercises",
  "exercise-detail",
  "metrics",
  "profile",
  "insights",
  "prs",
  "achievements",
  "nutrition",
  "export",
  "export-ai",
  "export-ai-apply",
];

const messages = {
  common: {
    loading: "Loading",
    save: "Save",
    cancel: "Cancel",
    delete: "Delete",
    edit: "Edit",
  },
};

function renderWithIntl(variant: RouteSkeletonVariant) {
  return render(
    <NextIntlClientProvider locale="en" messages={messages}>
      <RouteSkeleton variant={variant} />
    </NextIntlClientProvider>
  );
}

describe("RouteSkeleton", () => {
  for (const variant of VARIANTS) {
    it(`renders an a11y-tagged status wrapper for variant "${variant}"`, () => {
      const { container } = renderWithIntl(variant);

      const wrapper = screen.getByRole("status", { name: /loading/i });
      expect(wrapper.getAttribute("aria-busy")).toBe("true");
      expect(wrapper.getAttribute("data-testid")).toBe(
        `route-skeleton-${variant}`
      );

      const pulse = container.querySelector(".animate-pulse");
      expect(pulse).not.toBeNull();
    });
  }
});
