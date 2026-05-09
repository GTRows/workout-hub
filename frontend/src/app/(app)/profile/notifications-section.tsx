"use client";

import { useEffect, useState } from "react";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { Card, CardDescription, CardTitle } from "@/components/ui/card";
import { PrToast } from "@/components/pr-toast";
import { api } from "@/lib/api/client";
import { subscribePush } from "@/lib/push/subscribe";

type Status = "unsupported" | "default" | "granted" | "denied";

function readStatus(): Status {
  if (typeof window === "undefined") return "unsupported";
  if (typeof window.Notification === "undefined") return "unsupported";
  if (!("serviceWorker" in navigator)) return "unsupported";
  return Notification.permission as Status;
}

export function NotificationsSection() {
  const t = useTranslations("profile.notifications");
  const tTest = useTranslations("push.test");
  const [status, setStatus] = useState<Status>("unsupported");
  const [subscribed, setSubscribed] = useState<boolean>(false);
  const [busy, setBusy] = useState<boolean>(false);
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  useEffect(() => {
    const initial = readStatus();
    setStatus(initial);
    if (initial !== "granted") return;
    let cancelled = false;
    navigator.serviceWorker.ready
      .then((reg) => reg.pushManager.getSubscription())
      .then((sub) => {
        if (!cancelled) setSubscribed(sub !== null);
      })
      .catch(() => {
        /* no-op; the section just shows the un-subscribed state */
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const onEnable = async () => {
    setBusy(true);
    try {
      const res = await Notification.requestPermission();
      if (res !== "granted") {
        setStatus(res as Status);
        return;
      }
      await subscribePush();
      setSubscribed(true);
      setStatus("granted");
    } finally {
      setBusy(false);
    }
  };

  const onDisable = async () => {
    setBusy(true);
    try {
      const reg = await navigator.serviceWorker.ready;
      const sub = await reg.pushManager.getSubscription();
      if (!sub) {
        setSubscribed(false);
        return;
      }
      const endpoint = sub.endpoint;
      await sub.unsubscribe();
      await api.request({
        method: "DELETE",
        path: `/api/push/subscribe?endpoint=${encodeURIComponent(endpoint)}`,
      });
      setSubscribed(false);
    } finally {
      setBusy(false);
    }
  };

  const onTest = async () => {
    setBusy(true);
    try {
      const r = await api.request<{ delivered: number; subscriptions: number }>({
        method: "POST",
        path: "/api/push/test",
      });
      if (r.subscriptions === 0) {
        setToastMessage(tTest("zero"));
      } else {
        setToastMessage(tTest("success", { count: r.delivered }));
      }
    } catch {
      setToastMessage(tTest("error"));
    } finally {
      setBusy(false);
    }
  };

  const statusKey =
    status === "granted"
      ? "statusGranted"
      : status === "default"
      ? "statusDefault"
      : status === "denied"
      ? "statusDenied"
      : "statusUnsupported";

  return (
    <Card className="space-y-3" data-testid="notifications-section">
      <div className="space-y-1">
        <CardTitle className="text-lg">{t("title")}</CardTitle>
        <CardDescription>{t("description")}</CardDescription>
      </div>
      <p
        className="text-sm text-muted-foreground"
        data-testid="notifications-status"
      >
        {t(statusKey)}
      </p>

      {status === "default" && (
        <Button onClick={onEnable} disabled={busy} size="sm">
          {t("title")}
        </Button>
      )}

      {status === "granted" && subscribed && (
        <div className="flex flex-wrap gap-2">
          <Button variant="outline" onClick={onDisable} disabled={busy} size="sm">
            {t("disable")}
          </Button>
          <Button variant="outline" onClick={onTest} disabled={busy} size="sm">
            {t("testButton")}
          </Button>
        </div>
      )}

      {status === "granted" && !subscribed && (
        <Button onClick={onEnable} disabled={busy} size="sm">
          {t("title")}
        </Button>
      )}

      {toastMessage && (
        <PrToast
          message={toastMessage}
          onDismiss={() => setToastMessage(null)}
        />
      )}
    </Card>
  );
}
