#!/usr/bin/env node
// Per-route bundle-size gate. Walks the webpack-emitted manifests under
// frontend/.next, computes the gzip-compressed first-load JS size per
// route, fails with exit 1 if any route exceeds the threshold.
//
// Threshold source-of-truth: docs/PERF_BUDGETS.md (250KB compressed).
// Run AFTER `pnpm build` (or `pnpm build --webpack`); the script reads
// the build output directly.
import { readFileSync, readdirSync } from "node:fs";
import { join, resolve, dirname } from "node:path";
import { gzipSync } from "node:zlib";
import { fileURLToPath } from "node:url";

const ROUTE_LIMIT_BYTES = 250 * 1024; // 250KB compressed

const __filename = fileURLToPath(import.meta.url);
const repoRoot = resolve(dirname(__filename), "..");
const buildDir = join(repoRoot, "frontend", ".next");

function readJson(path) {
  return JSON.parse(readFileSync(path, "utf8"));
}

function gzipSizeOf(absPath) {
  const raw = readFileSync(absPath);
  return gzipSync(raw, { level: 9 }).byteLength;
}

function listFilesRec(dir) {
  const out = [];
  for (const entry of readdirSync(dir, { withFileTypes: true })) {
    const full = join(dir, entry.name);
    if (entry.isDirectory()) out.push(...listFilesRec(full));
    else if (entry.isFile() && entry.name.endsWith(".js")) out.push(full);
  }
  return out;
}

function main() {
  const buildManifest = readJson(join(buildDir, "build-manifest.json"));
  const appRoutes = readJson(
    join(buildDir, "app-path-routes-manifest.json")
  );

  const rootMain = (buildManifest.rootMainFiles || []).map((f) =>
    join(buildDir, f)
  );
  const rootMainSize = rootMain.reduce(
    (acc, f) => acc + gzipSizeOf(f),
    0
  );

  const routes = [];
  for (const [page, urlPath] of Object.entries(appRoutes)) {
    if (page.startsWith("/api/")) continue; // API routes have no client bundle
    const segDir = page.replace(/\/page$/, "").replace(/^\//, "");
    const chunkDir = join(buildDir, "static", "chunks", "app", segDir);
    let chunkFiles = [];
    try {
      chunkFiles = listFilesRec(chunkDir);
    } catch {
      // Some routes have no per-segment chunk dir (purely SSR). Treat
      // their first-load JS as rootMain only.
      chunkFiles = [];
    }
    const segSize = chunkFiles.reduce(
      (acc, f) => acc + gzipSizeOf(f),
      0
    );
    const total = rootMainSize + segSize;
    routes.push({ page, urlPath, segSize, total });
  }

  routes.sort((a, b) => b.total - a.total);

  const fail = [];
  console.log(
    "Route                                          Segment KB  Total KB"
  );
  console.log(
    "-------------------------------------------- ---------- ---------"
  );
  for (const r of routes) {
    const segKb = (r.segSize / 1024).toFixed(1);
    const totKb = (r.total / 1024).toFixed(1);
    console.log(
      `${r.urlPath.padEnd(44)} ${segKb.padStart(10)} ${totKb.padStart(9)}`
    );
    if (r.total > ROUTE_LIMIT_BYTES) fail.push(r);
  }
  console.log("");
  console.log(
    `Root-main shared (counted in every route total): ${(
      rootMainSize / 1024
    ).toFixed(1)} KB`
  );
  console.log(
    `Per-route ceiling: ${(ROUTE_LIMIT_BYTES / 1024).toFixed(0)} KB compressed`
  );

  if (fail.length > 0) {
    console.error("");
    console.error(
      `BUDGET FAIL: ${fail.length} route(s) exceed the 250KB compressed ceiling:`
    );
    for (const r of fail) {
      console.error(
        `  ${r.urlPath} -> ${(r.total / 1024).toFixed(1)} KB`
      );
    }
    process.exit(1);
  }

  console.log("");
  console.log("BUDGET OK: all routes under the 250KB compressed ceiling.");
}

main();
