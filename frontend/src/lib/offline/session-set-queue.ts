import Dexie, { type Table } from "dexie";
import type { AddSetPayload } from "@/lib/api/endpoints";

export type QueuedSet = {
  id?: number;
  sessionId: string;
  payload: AddSetPayload;
  createdAt: number;
};

class OfflineDb extends Dexie {
  queuedSets!: Table<QueuedSet, number>;

  constructor() {
    super("workouthub-offline");
    this.version(1).stores({
      // Per-payload `clientSetId` (Phase 15-04 backend; threaded by Phase 25-01
      // frontend) is the primary idempotency key -- the backend looks up
      // `findBySessionIdAndClientSetId` and returns the existing row on replay.
      // Fallback dedup: the DB UNIQUE(session_id, exercise_id, set_number)
      // constraint stops real duplicates if the client somehow omits clientSetId.
      // Either way, flushing treats a 409 as 'already saved' and drops the
      // queued item.
      queuedSets: "++id, sessionId",
    });
  }
}

// Lazy singleton: keep SSR out of the Dexie init path.
let dbSingleton: OfflineDb | null = null;
export function getOfflineDb(): OfflineDb {
  if (typeof indexedDB === "undefined") {
    throw new Error("IndexedDB not available in this environment");
  }
  if (!dbSingleton) {
    dbSingleton = new OfflineDb();
  }
  return dbSingleton;
}

// Exposed for tests to force a fresh DB.
export function __resetOfflineDb(): void {
  dbSingleton = null;
}

export async function enqueueSet(
  sessionId: string,
  payload: AddSetPayload
): Promise<number> {
  const db = getOfflineDb();
  return db.queuedSets.add({ sessionId, payload, createdAt: Date.now() });
}

export async function queuedCount(sessionId: string): Promise<number> {
  const db = getOfflineDb();
  return db.queuedSets.where("sessionId").equals(sessionId).count();
}

export async function queuedForSession(sessionId: string): Promise<QueuedSet[]> {
  const db = getOfflineDb();
  return db.queuedSets.where("sessionId").equals(sessionId).sortBy("id");
}

export type PostOutcome = { ok: true } | { ok: false; status: number };

export type DrainResult = { drained: number; dropped: number; remaining: number };

/**
 * Drains queued sets for a session by invoking postFn one row at a time.
 * - 2xx or 409 (duplicate per UNIQUE constraint) -> drop the item and count.
 * - Other HTTP error -> stop the drain and leave remaining items for retry.
 * - Network failure (postFn throws) -> stop the drain.
 */
export async function drainForSession(
  sessionId: string,
  postFn: (payload: AddSetPayload) => Promise<PostOutcome>
): Promise<DrainResult> {
  const db = getOfflineDb();
  const items = await db.queuedSets
    .where("sessionId")
    .equals(sessionId)
    .sortBy("id");

  let drained = 0;
  let dropped = 0;
  for (const item of items) {
    let outcome: PostOutcome;
    try {
      outcome = await postFn(item.payload);
    } catch {
      break;
    }
    if (outcome.ok) {
      await db.queuedSets.delete(item.id!);
      drained++;
      continue;
    }
    if (outcome.status === 409) {
      await db.queuedSets.delete(item.id!);
      dropped++;
      continue;
    }
    // Leave the rest; surfaced to caller as remaining.
    break;
  }
  const remaining = await queuedCount(sessionId);
  return { drained, dropped, remaining };
}

export function subscribeOnline(handler: () => void): () => void {
  if (typeof window === "undefined") return () => {};
  window.addEventListener("online", handler);
  return () => window.removeEventListener("online", handler);
}
