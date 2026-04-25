"use client";

import { useMutation } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Card, CardDescription, CardTitle } from "@/components/ui/card";
import {
  importAppleHealth,
  importGarminFit,
  importGoogleFit,
} from "@/lib/api/endpoints";
import type { HealthImportResult } from "@/lib/api/schemas";

type Source = "apple" | "google" | "garmin";

export function HealthImportClient() {
  const t = useTranslations("health");
  const [bodyMass, setBodyMass] = useState(true);
  const [workouts, setWorkouts] = useState(true);
  const [result, setResult] = useState<{ source: Source; data: HealthImportResult } | null>(
    null
  );
  const [error, setError] = useState<string | null>(null);

  const mutation = useMutation({
    mutationFn: async ({ source, file }: { source: Source; file: File }) => {
      let data: HealthImportResult;
      if (source === "apple") {
        data = await importAppleHealth(file, { bodyMass, workouts });
      } else if (source === "google") {
        data = await importGoogleFit(file, { bodyMass, workouts });
      } else {
        data = await importGarminFit(file);
      }
      return { source, data } as { source: Source; data: HealthImportResult };
    },
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
        <CardTitle className="text-lg">{t("title")}</CardTitle>
        <CardDescription>{t("description")}</CardDescription>
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

      <div className="space-y-2">
        <p className="text-sm font-medium">{t("appleTitle")}</p>
        <p className="text-xs text-muted-foreground">{t("appleDescription")}</p>
        <input
          type="file"
          accept=".xml,.zip,application/xml,application/zip"
          aria-label={t("filePickerLabel")}
          data-testid="apple-file-input"
          onChange={(e) => {
            const file = e.target.files?.[0];
            if (file) mutation.mutate({ source: "apple", file });
            e.target.value = "";
          }}
          className="block text-sm"
        />
      </div>

      <div className="space-y-2 pt-2 border-t border-border">
        <p className="text-sm font-medium">{t("googleTitle")}</p>
        <p className="text-xs text-muted-foreground">{t("googleDescription")}</p>
        <input
          type="file"
          accept=".json,application/json"
          aria-label={t("googleFilePickerLabel")}
          data-testid="google-file-input"
          onChange={(e) => {
            const file = e.target.files?.[0];
            if (file) mutation.mutate({ source: "google", file });
            e.target.value = "";
          }}
          className="block text-sm"
        />
      </div>

      <div className="space-y-2 pt-2 border-t border-border">
        <p className="text-sm font-medium">{t("garminTitle")}</p>
        <p className="text-xs text-muted-foreground">{t("garminDescription")}</p>
        <input
          type="file"
          accept=".fit,application/octet-stream"
          aria-label={t("garminFilePickerLabel")}
          data-testid="garmin-file-input"
          onChange={(e) => {
            const file = e.target.files?.[0];
            if (file) mutation.mutate({ source: "garmin", file });
            e.target.value = "";
          }}
          className="block text-sm"
        />
      </div>

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
                bm: result.data.bodyMassImported,
                wo: result.data.workoutsImported,
              })}
            </span>
          </Button>
          <p className="text-xs text-muted-foreground">
            {t("skipped", {
              bm: result.data.bodyMassSkipped,
              wo: result.data.workoutsSkipped,
            })}
          </p>
        </div>
      )}
    </Card>
  );
}
