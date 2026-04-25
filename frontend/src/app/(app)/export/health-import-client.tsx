"use client";

import { useMutation } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Card, CardDescription, CardTitle } from "@/components/ui/card";
import { importAppleHealth } from "@/lib/api/endpoints";
import type { HealthImportResult } from "@/lib/api/schemas";

export function HealthImportClient() {
  const t = useTranslations("health");
  const [bodyMass, setBodyMass] = useState(true);
  const [workouts, setWorkouts] = useState(true);
  const [result, setResult] = useState<HealthImportResult | null>(null);
  const [error, setError] = useState<string | null>(null);

  const mutation = useMutation({
    mutationFn: (file: File) =>
      importAppleHealth(file, { bodyMass, workouts }),
    onSuccess: (r) => {
      setResult(r);
      setError(null);
    },
    onError: (err: unknown) => {
      const message = err instanceof Error ? err.message : String(err);
      setError(message || t("error"));
      setResult(null);
    },
  });

  return (
    <Card className="space-y-4" data-testid="health-import-card">
      <div className="space-y-1">
        <CardTitle className="text-lg">{t("appleTitle")}</CardTitle>
        <CardDescription>{t("appleDescription")}</CardDescription>
      </div>
      <div className="flex flex-wrap gap-4 text-sm">
        <label className="flex items-center gap-2">
          <input
            type="checkbox"
            checked={bodyMass}
            onChange={(e) => setBodyMass(e.target.checked)}
            data-testid="apple-toggle-body-mass"
          />
          {t("toggleBodyMass")}
        </label>
        <label className="flex items-center gap-2">
          <input
            type="checkbox"
            checked={workouts}
            onChange={(e) => setWorkouts(e.target.checked)}
            data-testid="apple-toggle-workouts"
          />
          {t("toggleWorkouts")}
        </label>
      </div>
      <input
        type="file"
        accept=".xml,.zip,application/xml,application/zip"
        aria-label={t("filePickerLabel")}
        data-testid="apple-file-input"
        onChange={(e) => {
          const file = e.target.files?.[0];
          if (file) mutation.mutate(file);
          e.target.value = "";
        }}
        className="block text-sm"
      />
      {mutation.isPending && (
        <p className="text-xs text-muted-foreground">{t("uploading")}</p>
      )}
      {error && (
        <p role="alert" className="text-sm text-destructive">
          {error}
        </p>
      )}
      {result && !error && (
        <div className="space-y-1 text-sm" data-testid="apple-import-result">
          <Button asChild variant="outline" size="sm" disabled>
            <span>
              {t("imported", {
                bm: result.bodyMassImported,
                wo: result.workoutsImported,
              })}
            </span>
          </Button>
          <p className="text-xs text-muted-foreground">
            {t("skipped", {
              bm: result.bodyMassSkipped,
              wo: result.workoutsSkipped,
            })}
          </p>
        </div>
      )}
    </Card>
  );
}
