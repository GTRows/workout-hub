import { readFileSync } from "node:fs";
import { join } from "node:path";
import { describe, expect, it } from "vitest";

describe("service worker contract", () => {
  it("declares wh-shell-v2 cache, /offline + manifest + icon shell, and /api/ bypass", () => {
    const raw = readFileSync(
      join(process.cwd(), "public", "sw.js"),
      "utf8"
    );

    expect(raw).toContain('const CACHE = "wh-shell-v2";');
    expect(raw).toContain('"/offline"');
    expect(raw).toContain('"/manifest.webmanifest"');
    expect(raw).toContain('"/icons/icon-192.png"');
    expect(raw).toMatch(/url\.pathname\.startsWith\(\s*"\/api\/"\s*\)/);
    expect(raw).toContain('caches.match("/offline")');
  });
});
