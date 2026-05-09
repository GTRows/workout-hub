import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { Skeleton } from "./skeleton";

describe("Skeleton", () => {
  it("renders a presentational div with the pulse class set", () => {
    render(<Skeleton data-testid="s" />);
    const el = screen.getByTestId("s");
    expect(el.tagName).toBe("DIV");
    expect(el.getAttribute("role")).toBe("presentation");
    expect(el.getAttribute("aria-hidden")).toBe("true");
    expect(el.className).toContain("animate-pulse");
    expect(el.className).toContain("rounded-md");
    expect(el.className).toContain("bg-muted");
  });

  it("merges custom className with the base classes via cn", () => {
    render(<Skeleton data-testid="s" className="h-4 w-1/2" />);
    const el = screen.getByTestId("s");
    expect(el.className).toContain("animate-pulse");
    expect(el.className).toContain("h-4");
    expect(el.className).toContain("w-1/2");
  });

  it("forwards arbitrary data-* attributes", () => {
    render(<Skeleton data-testid="s" data-variant="card" />);
    const el = screen.getByTestId("s");
    expect(el.getAttribute("data-variant")).toBe("card");
  });
});
