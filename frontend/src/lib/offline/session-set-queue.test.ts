import "fake-indexeddb/auto";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  __resetOfflineDb,
  drainForSession,
  enqueueSet,
  getOfflineDb,
  queuedCount,
  queuedForSession,
  subscribeOnline,
} from "@/lib/offline/session-set-queue";

const sessionId = "ffffffff-1111-1111-1111-111111111111";
const otherSessionId = "ffffffff-2222-2222-2222-222222222222";
const exerciseId = "ffffffff-3333-3333-3333-333333333333";

function payload(setNumber: number, reps = 10, weightKg = 50) {
  return {
    exerciseId,
    setNumber,
    repsDone: reps,
    weightKg,
    completed: true,
  };
}

describe("offline session-set queue", () => {
  beforeEach(async () => {
    __resetOfflineDb();
    await getOfflineDb().queuedSets.clear();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("enqueue stores items scoped to the session", async () => {
    await enqueueSet(sessionId, payload(1));
    await enqueueSet(sessionId, payload(2));
    await enqueueSet(otherSessionId, payload(1));

    expect(await queuedCount(sessionId)).toBe(2);
    expect(await queuedCount(otherSessionId)).toBe(1);

    const items = await queuedForSession(sessionId);
    expect(items.map((i) => i.payload.setNumber)).toEqual([1, 2]);
  });

  it("drains queued items when post succeeds", async () => {
    await enqueueSet(sessionId, payload(1));
    await enqueueSet(sessionId, payload(2));

    const postFn = vi.fn().mockResolvedValue({ ok: true });
    const result = await drainForSession(sessionId, postFn);

    expect(result).toEqual({ drained: 2, dropped: 0, permanentlyDropped: 0, remaining: 0 });
    expect(postFn).toHaveBeenCalledTimes(2);
    expect(await queuedCount(sessionId)).toBe(0);
  });

  it("treats 409 as already-saved and drops the item without failing the drain", async () => {
    await enqueueSet(sessionId, payload(1));
    await enqueueSet(sessionId, payload(2));

    const postFn = vi
      .fn()
      .mockResolvedValueOnce({ ok: false, status: 409 })
      .mockResolvedValueOnce({ ok: true });
    const result = await drainForSession(sessionId, postFn);

    expect(result).toEqual({ drained: 1, dropped: 1, permanentlyDropped: 0, remaining: 0 });
  });

  it("stops draining on a non-409 HTTP error and leaves subsequent items", async () => {
    await enqueueSet(sessionId, payload(1));
    await enqueueSet(sessionId, payload(2));
    await enqueueSet(sessionId, payload(3));

    const postFn = vi
      .fn()
      .mockResolvedValueOnce({ ok: true })
      .mockResolvedValueOnce({ ok: false, status: 500 });
    const result = await drainForSession(sessionId, postFn);

    expect(result.drained).toBe(1);
    expect(result.remaining).toBe(2);
    expect(postFn).toHaveBeenCalledTimes(2);
  });

  it("stops draining when postFn throws a network error", async () => {
    await enqueueSet(sessionId, payload(1));
    await enqueueSet(sessionId, payload(2));

    const postFn = vi
      .fn()
      .mockResolvedValueOnce({ ok: true })
      .mockRejectedValueOnce(new Error("network down"));
    const result = await drainForSession(sessionId, postFn);

    expect(result.drained).toBe(1);
    expect(result.remaining).toBe(1);
  });

  it("a subsequent drain retries the remaining item", async () => {
    await enqueueSet(sessionId, payload(1));

    const failingPost = vi.fn().mockRejectedValue(new Error("offline"));
    await drainForSession(sessionId, failingPost);
    expect(await queuedCount(sessionId)).toBe(1);

    const successfulPost = vi.fn().mockResolvedValue({ ok: true });
    const result = await drainForSession(sessionId, successfulPost);
    expect(result.drained).toBe(1);
    expect(await queuedCount(sessionId)).toBe(0);
  });

  it("subscribeOnline invokes the handler when the window online event fires", () => {
    const handler = vi.fn();
    const unsubscribe = subscribeOnline(handler);

    window.dispatchEvent(new Event("online"));
    expect(handler).toHaveBeenCalledTimes(1);

    unsubscribe();
    window.dispatchEvent(new Event("online"));
    expect(handler).toHaveBeenCalledTimes(1);
  });

  it("drops a permanent-reject item and continues draining the rest", async () => {
    await enqueueSet(sessionId, payload(1));
    await enqueueSet(sessionId, payload(2));
    await enqueueSet(sessionId, payload(3));

    const postFn = vi
      .fn()
      .mockResolvedValueOnce({ ok: true })
      .mockResolvedValueOnce({ ok: false, permanent: true, reason: "SESSION_FINISHED" })
      .mockResolvedValueOnce({ ok: true });
    const result = await drainForSession(sessionId, postFn);

    expect(result).toEqual({
      drained: 2,
      dropped: 0,
      permanentlyDropped: 1,
      remaining: 0,
    });
    expect(postFn).toHaveBeenCalledTimes(3);
    expect(await queuedCount(sessionId)).toBe(0);
  });
});
