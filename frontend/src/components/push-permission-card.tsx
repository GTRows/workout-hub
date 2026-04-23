"use client";

import { useEffect, useState } from "react";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { Card, CardDescription, CardTitle } from "@/components/ui/card";
import { subscribePush } from "@/lib/push/subscribe";

type Status = "unsupported" | "default" | "granted" | "denied" | "pending";

function readPermission(): Status {
  if (typeof window === "undefined") return "unsupported";
  if (typeof window.Notification === "undefined") return "unsupported";
  if (!("serviceWorker" in navigator)) return "unsupported";
  return Notification.permission as Status;
}

export function PushPermissionCard() {
  const t = useTranslations("push");
  const [status, setStatus] = useState<Status>("unsupported");

  useEffect(() => {
    setStatus(readPermission());
  }, []);

  if (status === "unsupported" || status === "denied") return null;
  if (status === "granted") return null;

  const request = async () => {
    setStatus("pending");
    try {
      const result = await Notification.requestPermission();
      if (result !== "granted") {
        setStatus(result as Status);
        return;
      }
      await subscribePush();
      setStatus("granted");
    } catch {
      setStatus(readPermission());
    }
  };

  return (
    <Card className="space-y-2" data-testid="push-permission-card">
      <CardTitle className="text-base">{t("title")}</CardTitle>
      <CardDescription>{t("description")}</CardDescription>
      <Button onClick={request} disabled={status === "pending"} size="sm">
        {status === "pending" ? t("pending") : t("enable")}
      </Button>
    </Card>
  );
}
