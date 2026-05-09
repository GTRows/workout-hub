"use client";

import { useMutation } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { useRef, useState } from "react";
import {
  fetchClaudeSummary,
  fetchFullExport,
  fetchSectionExport,
  fetchSessionsCsv,
  importFullDump,
  type ExportSection,
  type ImportResult,
} from "@/lib/api/endpoints";

const SECTIONS: ExportSection[] = [
  "profile",
  "plans",
  "sessions",
  "metrics",
  "supplements",
];
import { Button } from "@/components/ui/button";
import { Card, CardDescription, CardTitle } from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import { ExportHistoryCard } from "./export-history-card";
import { appendHistoryEntry } from "./export-history";

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
      const filename = `workouthub-claude-${iso}.json`;
      downloadJson(body, filename);
      appendHistoryEntry({ kind: "claude", filename });
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
      const filename = `workouthub-full-${iso}.json`;
      downloadJson(body, filename);
      appendHistoryEntry({ kind: "full", filename });
      setFullDownloadedAt(new Date().toLocaleString());
      setFullError(null);
    },
    onError: () => setFullError(t("fullError")),
  });

  const [sectionPending, setSectionPending] = useState<ExportSection | null>(null);
  const [sectionError, setSectionError] = useState<string | null>(null);
  const [csvPending, setCsvPending] = useState(false);
  const downloadCsv = async () => {
    setCsvPending(true);
    setSectionError(null);
    try {
      const csv = await fetchSessionsCsv();
      const blob = new Blob([csv], { type: "text/csv;charset=utf-8" });
      const url = URL.createObjectURL(blob);
      try {
        const iso = new Date().toISOString().slice(0, 10);
        const filename = `workouthub-sessions-${iso}.csv`;
        const a = document.createElement("a");
        a.href = url;
        a.download = filename;
        document.body.appendChild(a);
        a.click();
        a.remove();
        appendHistoryEntry({ kind: "csv", filename });
      } finally {
        URL.revokeObjectURL(url);
      }
    } catch {
      setSectionError(t("csvError"));
    } finally {
      setCsvPending(false);
    }
  };
  const downloadSection = async (section: ExportSection) => {
    setSectionPending(section);
    setSectionError(null);
    try {
      const body = await fetchSectionExport(section);
      const iso = new Date().toISOString().slice(0, 10);
      const filename = `workouthub-${section}-${iso}.json`;
      downloadJson(body, filename);
      appendHistoryEntry({ kind: "section", filename, sectionLabel: section });
    } catch {
      setSectionError(t("sectionError", { section }));
    } finally {
      setSectionPending(null);
    }
  };

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
    const ok = window.confirm(t("importConfirm", { file: file.name }));
    if (!ok) return;
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

      <Card className="space-y-3" data-testid="section-export-card">
        <div className="space-y-1">
          <CardTitle className="text-lg">{t("sectionTitle")}</CardTitle>
          <CardDescription>{t("sectionDescription")}</CardDescription>
        </div>
        <div className="flex flex-wrap gap-2">
          {SECTIONS.map((section) => (
            <Button
              key={section}
              variant="outline"
              size="sm"
              onClick={() => downloadSection(section)}
              disabled={sectionPending !== null}
              data-testid={`section-${section}`}
            >
              {sectionPending === section
                ? t("sectionPending")
                : t(`sectionButton.${section}`)}
            </Button>
          ))}
        </div>
        {sectionError && (
          <p role="alert" className="text-sm text-destructive">
            {sectionError}
          </p>
        )}
        <div className="pt-2 border-t border-border">
          <Button
            variant="outline"
            size="sm"
            onClick={downloadCsv}
            disabled={csvPending}
            data-testid="csv-sessions"
          >
            {csvPending ? t("csvPending") : t("csvButton")}
          </Button>
          <p className="mt-1 text-xs text-muted-foreground">
            {t("csvHint")}
          </p>
        </div>
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
          <div className="space-y-2">
            <p
              className="text-sm text-primary"
              data-testid="import-success"
            >
              {t("importSuccess", {
                metrics: importResult.metricsInserted,
                supplements: importResult.supplementsInserted,
                plans: importResult.plansInserted,
                sessions: importResult.sessionsInserted,
              })}
            </p>
            {importResult.warnings && importResult.warnings.length > 0 && (
              <div data-testid="import-warnings" className="space-y-1">
                <p className="text-xs font-medium text-muted-foreground">
                  {t("importWarningsHeading")}
                </p>
                <ul className="list-disc pl-5 text-xs text-muted-foreground">
                  {importResult.warnings.map((w, i) => (
                    <li key={i}>{w}</li>
                  ))}
                </ul>
              </div>
            )}
            {importResult.suggestions && importResult.suggestions.length > 0 && (
              <div data-testid="import-suggestions" className="space-y-1">
                <p className="text-xs font-medium text-muted-foreground">
                  {t("importSuggestionsHeading")}
                </p>
                <ul className="list-disc pl-5 text-xs text-muted-foreground">
                  {importResult.suggestions.map((s, i) => (
                    <li key={i}>{s}</li>
                  ))}
                </ul>
              </div>
            )}
          </div>
        )}
      </Card>

      <ExportHistoryCard />
    </div>
  );
}
