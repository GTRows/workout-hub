"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { cancelRestTimer, scheduleRestTimer } from "@/lib/api/endpoints";

type RestTimer = {
  start: (seconds: number) => void;
  stop: () => void;
  secondsRemaining: number | null;
};

type PushWiring = {
  sessionId: string;
  title: string;
  body: (seconds: number) => string;
};

const noop = () => {};

export function useRestTimer(
  onElapsed?: (seconds: number) => void,
  push?: PushWiring
): RestTimer {
  const [remaining, setRemaining] = useState<number | null>(null);
  const intervalRef = useRef<number | null>(null);
  const durationRef = useRef<number>(0);

  const clearInterval = useCallback(() => {
    if (intervalRef.current) {
      window.clearInterval(intervalRef.current);
      intervalRef.current = null;
    }
  }, []);

  const cancelServerSchedule = useCallback(() => {
    if (!push) return;
    // Fire-and-forget: a failed cancel must not block the UI. The server
    // will still dispatch the row when it elapses, but the foreground
    // notification path is the one the user sees in the visible-tab case.
    cancelRestTimer(push.sessionId).catch(noop);
  }, [push]);

  const stop = useCallback(() => {
    clearInterval();
    setRemaining(null);
    cancelServerSchedule();
  }, [clearInterval, cancelServerSchedule]);

  const start = useCallback(
    (seconds: number) => {
      clearInterval();
      if (seconds <= 0) return;
      durationRef.current = seconds;
      setRemaining(seconds);
      if (push) {
        // Cancel any prior schedule for this session, then post the new one.
        // Both calls are best-effort: a failed POST does not block the UI.
        cancelRestTimer(push.sessionId)
          .catch(noop)
          .then(() =>
            scheduleRestTimer(push.sessionId, {
              seconds,
              title: push.title,
              body: push.body(seconds),
            }).catch(noop)
          );
      }
      intervalRef.current = window.setInterval(() => {
        setRemaining((prev) => {
          if (prev === null) return null;
          if (prev <= 1) {
            clearInterval();
            onElapsed?.(durationRef.current);
            return 0;
          }
          return prev - 1;
        });
      }, 1000);
    },
    [clearInterval, onElapsed, push]
  );

  useEffect(() => () => clearInterval(), [clearInterval]);

  return { start, stop, secondsRemaining: remaining };
}

export function notifyRestElapsed(seconds: number): void {
  if (typeof window === "undefined") return;
  if (!("Notification" in window)) return;
  if (Notification.permission !== "granted") return;

  if ("serviceWorker" in navigator && navigator.serviceWorker.controller) {
    navigator.serviceWorker.ready
      .then((reg) =>
        reg.showNotification("Rest over", {
          body: `${seconds}s rest complete - time for the next set`,
          icon: "/icons/icon-192.png",
          tag: "wh-rest-timer",
        })
      )
      .catch(() => {
        new Notification("Rest over", {
          body: `${seconds}s rest complete`,
        });
      });
    return;
  }

  new Notification("Rest over", {
    body: `${seconds}s rest complete`,
  });
}
