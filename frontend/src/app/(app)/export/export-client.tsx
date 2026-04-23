"use client";

import { useMutation } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { useState } from "react";
import { fetchClaudeSummary } from "@/lib/api/endpoints";
import { Button } from "@/components/ui/button";
import { Card, CardDescription, CardTitle } from "@/components/ui/card";
import { Label } from "@/components/ui/label";

export function ExportClient() {
  const t = useTranslations("export");
  const [days, setDays] = useState<number>(30);
  const [lastDownloadedAt, setLastDownloadedAt] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const mutation = useMutation({
    mutationFn: () => fetchClaudeSummary(days),
    onSuccess: (body) => {
      const json = JSON.stringify(body, null, 2);
      const blob = new Blob([json], { type: "application/json" });
      const url = URL.createObjectURL(blob);
      try {
        const iso = new Date().toISOString().slice(0, 10);
        const a = document.createElement("a");
        a.href = url;
        a.download = `workouthub-claude-${iso}.json`;
        document.body.appendChild(a);
        a.click();
        a.remove();
      } finally {
        URL.revokeObjectURL(url);
      }
      setLastDownloadedAt(new Date().toLocaleString());
      setError(null);
    },
    onError: () => {
      setError(t("error"));
    },
  });

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-semibold tracking-tight">{t("title")}</h1>

      <Card className="space-y-4">
        <div className="space-y-1">
          <CardTitle className="text-lg">{t("claudeTitle")}</CardTitle>
          <CardDescription>
            {t("claudeDescription", { days })}
          </CardDescription>
        </div>

        <div className="flex items-end gap-3">
          <div className="w-32 space-y-1">
            <Label htmlFor="export-days">{t("daysLabel")}</Label>
            <input
              id="export-days"
              type="number"
              min={1}
              max={365}
              value={days}
              onChange={(e) => {
                const n = Number(e.target.value);
                if (Number.isFinite(n)) setDays(Math.max(1, Math.min(365, n)));
              }}
              className="flex h-10 w-full rounded-md border border-border bg-transparent px-3 py-2 text-sm shadow-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary"
            />
          </div>
          <Button
            onClick={() => mutation.mutate()}
            disabled={mutation.isPending}
          >
            {mutation.isPending ? t("downloading") : t("downloadButton")}
          </Button>
        </div>

        {error && (
          <p role="alert" className="text-sm text-destructive">
            {error}
          </p>
        )}
        {lastDownloadedAt && !error && (
          <p className="text-xs text-muted-foreground">
            {t("lastDownloadedAt", { time: lastDownloadedAt })}
          </p>
        )}
      </Card>
    </div>
  );
}
