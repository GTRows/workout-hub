"use client";

import { useCallback, useEffect, useRef, useState } from "react";

type RestTimer = {
  start: (seconds: number) => void;
  stop: () => void;
  secondsRemaining: number | null;
};

export function useRestTimer(
  onElapsed?: (seconds: number) => void
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

  const stop = useCallback(() => {
    clearInterval();
    setRemaining(null);
  }, [clearInterval]);

  const start = useCallback(
    (seconds: number) => {
      clearInterval();
      if (seconds <= 0) return;
      durationRef.current = seconds;
      setRemaining(seconds);
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
    [clearInterval, onElapsed]
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
