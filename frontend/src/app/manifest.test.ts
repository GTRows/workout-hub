import { readFileSync } from "node:fs";
import { join } from "node:path";
import { describe, expect, it } from "vitest";

describe("PWA manifest", () => {
  it("declares name, short_name, icons, theme_color and standalone display", () => {
    const raw = readFileSync(
      join(process.cwd(), "public", "manifest.webmanifest"),
      "utf8"
    );
    const manifest = JSON.parse(raw);

    expect(manifest.name).toBe("WorkoutHub");
    expect(manifest.short_name).toBe("WorkoutHub");
    expect(manifest.display).toBe("standalone");
    expect(manifest.theme_color).toBe("#0ea5e9");
    expect(manifest.start_url).toBe("/dashboard");

    const icons = manifest.icons as Array<{
      sizes: string;
      type: string;
      purpose: string;
    }>;
    expect(icons.some((i) => i.sizes === "192x192")).toBe(true);
    expect(icons.some((i) => i.sizes === "512x512" && i.purpose === "any")).toBe(
      true
    );
    expect(
      icons.some((i) => i.sizes === "512x512" && i.purpose === "maskable")
    ).toBe(true);
  });
});
