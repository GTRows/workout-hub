import { z } from "zod";

const STORAGE_KEY = "wh.export.history";
const MAX_ENTRIES = 10;

const exportKindSchema = z.enum(["claude", "full", "section", "csv"]);

const historyEntrySchema = z.object({
  kind: exportKindSchema,
  filename: z.string().min(1),
  timestamp: z.number().int().nonnegative(),
  sectionLabel: z.string().optional(),
});

const historyArraySchema = z.array(historyEntrySchema).max(MAX_ENTRIES);

export type ExportKind = z.infer<typeof exportKindSchema>;
export type HistoryEntry = z.infer<typeof historyEntrySchema>;
export type NewHistoryEntry = Omit<HistoryEntry, "timestamp"> & {
  timestamp?: number;
};

const EMPTY_HISTORY: HistoryEntry[] = [];
let cachedRaw: string | null = null;
let cachedValue: HistoryEntry[] = EMPTY_HISTORY;

export function getHistory(): HistoryEntry[] {
  if (typeof window === "undefined") return EMPTY_HISTORY;
  let raw: string | null;
  try {
    raw = window.localStorage.getItem(STORAGE_KEY);
  } catch {
    return EMPTY_HISTORY;
  }
  if (raw === cachedRaw) return cachedValue;
  cachedRaw = raw;
  if (!raw) {
    cachedValue = EMPTY_HISTORY;
    return cachedValue;
  }
  try {
    const parsed = JSON.parse(raw);
    const validated = historyArraySchema.safeParse(parsed);
    cachedValue = validated.success ? validated.data : EMPTY_HISTORY;
  } catch {
    cachedValue = EMPTY_HISTORY;
  }
  return cachedValue;
}

const target = typeof window !== "undefined" ? new EventTarget() : null;
const SAME_TAB_EVENT = "wh.export.history.change";

export function appendHistoryEntry(entry: NewHistoryEntry): void {
  if (typeof window === "undefined") return;
  const current = getHistory();
  const next: HistoryEntry = {
    ...entry,
    timestamp: entry.timestamp ?? Date.now(),
  };
  const merged = [next, ...current].slice(0, MAX_ENTRIES);
  try {
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify(merged));
    target?.dispatchEvent(new Event(SAME_TAB_EVENT));
  } catch {
    // localStorage quota / privacy mode; silent failure is acceptable.
  }
}

export function clearHistory(): void {
  if (typeof window === "undefined") return;
  try {
    window.localStorage.removeItem(STORAGE_KEY);
    target?.dispatchEvent(new Event(SAME_TAB_EVENT));
  } catch {
    // ignore
  }
}

export function subscribeHistory(listener: () => void): () => void {
  if (typeof window === "undefined") return () => {};
  const sameTabHandler = () => listener();
  const crossTabHandler = (e: StorageEvent) => {
    if (e.key === STORAGE_KEY) listener();
  };
  target?.addEventListener(SAME_TAB_EVENT, sameTabHandler);
  window.addEventListener("storage", crossTabHandler);
  return () => {
    target?.removeEventListener(SAME_TAB_EVENT, sameTabHandler);
    window.removeEventListener("storage", crossTabHandler);
  };
}
