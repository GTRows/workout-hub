import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  appendHistoryEntry,
  clearHistory,
  getHistory,
  subscribeHistory,
} from "./export-history";

const STORAGE_KEY = "wh.export.history";

describe("export-history", () => {
  beforeEach(() => {
    window.localStorage.clear();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("returns [] when localStorage is empty", () => {
    expect(getHistory()).toEqual([]);
  });

  it("appends to the front and bounds to 10 entries", () => {
    for (let i = 0; i < 12; i++) {
      appendHistoryEntry({
        kind: "full",
        filename: `f${i}.json`,
        timestamp: 1_000 + i,
      });
    }
    const list = getHistory();
    expect(list).toHaveLength(10);
    expect(list[0].filename).toBe("f11.json");
    expect(list[9].filename).toBe("f2.json");
  });

  it("returns [] on JSON parse failure", () => {
    window.localStorage.setItem(STORAGE_KEY, "not-json{");
    expect(getHistory()).toEqual([]);
  });

  it("returns [] on schema parse failure", () => {
    window.localStorage.setItem(
      STORAGE_KEY,
      JSON.stringify([{ kind: "bogus", filename: 1 }]),
    );
    expect(getHistory()).toEqual([]);
  });

  it("clears the history", () => {
    appendHistoryEntry({ kind: "claude", filename: "a.json" });
    appendHistoryEntry({ kind: "csv", filename: "b.csv" });
    expect(getHistory()).toHaveLength(2);
    clearHistory();
    expect(getHistory()).toEqual([]);
  });

  it("notifies subscribers on append and clear and stops after unsubscribe", () => {
    const listener = vi.fn();
    const unsubscribe = subscribeHistory(listener);
    appendHistoryEntry({ kind: "claude", filename: "a.json" });
    expect(listener).toHaveBeenCalledTimes(1);
    clearHistory();
    expect(listener).toHaveBeenCalledTimes(2);
    unsubscribe();
    appendHistoryEntry({ kind: "claude", filename: "b.json" });
    expect(listener).toHaveBeenCalledTimes(2);
  });
});
