"use client";

import { useMutation } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { useRef, useState } from "react";
import {
  fetchClaudeSummary,
  fetchFullExport,
  importFullDump,
  type ImportResult,
} from "@/lib/api/endpoints";
import { Button } from "@/components/ui/button";
import { Card, CardDescription, CardTitle } from "@/components/ui/card";
import { Label } from "@/components/ui/label";

function downloadJson(payload: unknown, filename: string) {
  const json = JSON.stringify(payload, null, 2);
  const blob = new Blob([json], { type: "application/json" });
  const url = URL.createObjectURL(blob);
  try {
    const a = document.createElement("a");
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    a.remove();
  } finally {
    URL.revokeObjectURL(url);
  }
}

export function ExportClient() {
  const t = useTranslations("export");
  const [days, setDays] = useState<number>(30);
  const [lastDownloadedAt, setLastDownloadedAt] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const mutation = useMutation({
    mutationFn: () => fetchClaudeSummary(days),
    onSuccess: (body) => {
      const iso = new Date().toISOString().slice(0, 10);
      downloadJson(body, `workouthub-claude-${iso}.json`);
      setLastDownloadedAt(new Date().toLocaleString());
      setError(null);
    },
    onError: () => {
      setError(t("error"));
    },
  });

  const [fullDownloadedAt, setFullDownloadedAt] = useState<string | null>(null);
  const [fullError, setFullError] = useState<string | null>(null);
  const fullMutation = useMutation({
    mutationFn: () => fetchFullExport(),
    onSuccess: (body) => {
      const iso = new Date().toISOString().slice(0, 10);
      downloadJson(body, `workouthub-full-${iso}.json`);
      setFullDownloadedAt(new Date().toLocaleString());
      setFullError(null);
    },
    onError: () => setFullError(t("fullError")),
  });

  const fileInputRef = useRef<HTMLInputElement | null>(null);
  const [importResult, setImportResult] = useState<ImportResult | null>(null);
  const [importError, setImportError] = useState<string | null>(null);
  const importMutation = useMutation({
    mutationFn: (body: unknown) => importFullDump(body),
    onSuccess: (r) => {
      setImportResult(r);
      setImportError(null);
    },
    onError: (err: unknown) => {
      const message = err instanceof Error ? err.message : String(err);
      setImportError(message || t("importError"));
      setImportResult(null);
    },
  });

  const handleImportFile = (file: File) => {
    setImportError(null);
    setImportResult(null);
    const reader = new FileReader();
    reader.onload = () => {
      try {
        const text = String(reader.result ?? "");
        const parsed = JSON.parse(text);
        importMutation.mutate(parsed);
      } catch {
        setImportError(t("importParseError"));
      }
    };
    reader.onerror = () => setImportError(t("importParseError"));
    reader.readAsText(file);
  };

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

      <Card className="space-y-4" data-testid="full-export-card">
        <div className="space-y-1">
          <CardTitle className="text-lg">{t("fullTitle")}</CardTitle>
          <CardDescription>{t("fullDescription")}</CardDescription>
        </div>
        <Button
          onClick={() => fullMutation.mutate()}
          disabled={fullMutation.isPending}
        >
          {fullMutation.isPending ? t("fullDownloading") : t("fullButton")}
        </Button>
        {fullError && (
          <p role="alert" className="text-sm text-destructive">
            {fullError}
          </p>
        )}
        {fullDownloadedAt && !fullError && (
          <p className="text-xs text-muted-foreground">
            {t("lastDownloadedAt", { time: fullDownloadedAt })}
          </p>
        )}
      </Card>

      <Card className="space-y-4" data-testid="import-card">
        <div className="space-y-1">
          <CardTitle className="text-lg">{t("importTitle")}</CardTitle>
          <CardDescription>{t("importDescription")}</CardDescription>
        </div>
        <input
          ref={fileInputRef}
          type="file"
          accept="application/json,.json"
          aria-label={t("importFileLabel")}
          onChange={(e) => {
            const file = e.target.files?.[0];
            if (file) handleImportFile(file);
            e.target.value = "";
          }}
          className="block text-sm"
        />
        {importMutation.isPending && (
          <p className="text-xs text-muted-foreground">{t("importUploading")}</p>
        )}
        {importError && (
          <p role="alert" className="text-sm text-destructive">
            {importError}
          </p>
        )}
        {importResult && !importError && (
          <p
            className="text-sm text-primary"
            data-testid="import-success"
          >
            {t("importSuccess", {
              metrics: importResult.metricsInserted,
              supplements: importResult.supplementsInserted,
            })}
          </p>
        )}
      </Card>
    </div>
  );
}
