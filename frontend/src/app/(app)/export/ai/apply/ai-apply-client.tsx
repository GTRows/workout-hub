"use client";

import { useMutation } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { useState } from "react";
import {
  importFullDump,
  type ImportResult,
} from "@/lib/api/endpoints";
import { Button } from "@/components/ui/button";
import { Card, CardDescription, CardTitle } from "@/components/ui/card";

type Envelope = {
  schemaVersion?: number;
  user?: unknown;
  plans?: unknown[];
  sessions?: unknown[];
  bodyMetrics?: unknown[];
  supplements?: unknown[];
};

type Diff = {
  profile: "unchanged" | "overwritten";
  plans: number;
  sessions: number;
  bodyMetrics: number;
  supplements: number;
};

function summarize(dump: Envelope): Diff {
  return {
    profile: dump.user ? "overwritten" : "unchanged",
    plans: dump.plans?.length ?? 0,
    sessions: dump.sessions?.length ?? 0,
    bodyMetrics: dump.bodyMetrics?.length ?? 0,
    supplements: dump.supplements?.length ?? 0,
  };
}

export function AiApplyClient() {
  const t = useTranslations("ai");

  const [dump, setDump] = useState<Envelope | null>(null);
  const [parseError, setParseError] = useState<string | null>(null);
  const [fileName, setFileName] = useState<string>("");
  const [result, setResult] = useState<ImportResult | null>(null);

  const commit = useMutation({
    mutationFn: (body: Envelope) => importFullDump(body),
    onSuccess: (r) => setResult(r),
  });

  const onFile = (file: File) => {
    setParseError(null);
    setDump(null);
    setResult(null);
    setFileName(file.name);
    const reader = new FileReader();
    reader.onload = () => {
      try {
        const parsed = JSON.parse(String(reader.result ?? "")) as Envelope;
        if (parsed.schemaVersion !== 1) {
          setParseError(t("parseErrorSchema"));
          return;
        }
        setDump(parsed);
      } catch {
        setParseError(t("parseError"));
      }
    };
    reader.onerror = () => setParseError(t("parseError"));
    reader.readAsText(file);
  };

  const diff = dump ? summarize(dump) : null;

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-semibold tracking-tight">{t("applyTitle")}</h1>

      <Card className="space-y-3" data-testid="ai-apply-upload">
        <CardTitle className="text-lg">{t("uploadTitle")}</CardTitle>
        <CardDescription>{t("uploadDescription")}</CardDescription>
        <input
          type="file"
          accept="application/json,.json"
          aria-label={t("uploadLabel")}
          onChange={(e) => {
            const file = e.target.files?.[0];
            if (file) onFile(file);
            e.target.value = "";
          }}
          className="block text-sm"
        />
        {fileName && (
          <p className="text-xs text-muted-foreground">
            {t("fileChosen", { name: fileName })}
          </p>
        )}
        {parseError && (
          <p role="alert" className="text-sm text-destructive">
            {parseError}
          </p>
        )}
      </Card>

      {diff && !result && (
        <Card className="space-y-3" data-testid="ai-apply-diff">
          <CardTitle className="text-lg">{t("diffTitle")}</CardTitle>
          <CardDescription>{t("diffDescription")}</CardDescription>
          <ul className="text-sm space-y-1" data-testid="ai-apply-diff-list">
            <li>{t("diffProfile", { state: diff.profile })}</li>
            <li>{t("diffPlans", { count: diff.plans })}</li>
            <li>{t("diffSessions", { count: diff.sessions })}</li>
            <li>{t("diffMetrics", { count: diff.bodyMetrics })}</li>
            <li>{t("diffSupplements", { count: diff.supplements })}</li>
          </ul>
          <div className="flex gap-2">
            <Button
              onClick={() => commit.mutate(dump!)}
              disabled={commit.isPending}
              data-testid="ai-apply-commit"
            >
              {commit.isPending ? t("committing") : t("commitButton")}
            </Button>
            <Button variant="ghost" onClick={() => setDump(null)}>
              {t("cancelButton")}
            </Button>
          </div>
          {commit.isError && (
            <p role="alert" className="text-sm text-destructive">
              {t("commitError")}
            </p>
          )}
        </Card>
      )}

      {result && (
        <Card className="space-y-2" data-testid="ai-apply-committed">
          <CardTitle className="text-lg">{t("committedTitle")}</CardTitle>
          <p className="text-sm text-primary">
            {t("committedSummary", {
              metrics: result.metricsInserted,
              supplements: result.supplementsInserted,
              plans: result.plansInserted,
              sessions: result.sessionsInserted,
            })}
          </p>
          {result.warnings && result.warnings.length > 0 && (
            <ul className="list-disc pl-5 text-xs text-muted-foreground">
              {result.warnings.map((w, i) => (
                <li key={i}>{w}</li>
              ))}
            </ul>
          )}
        </Card>
      )}
    </div>
  );
}
