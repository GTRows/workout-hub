"use client";

import Link from "next/link";
import { useTranslations } from "next-intl";
import { useState } from "react";
import {
  fetchSectionExport,
  type ExportSection,
} from "@/lib/api/endpoints";
import { Button } from "@/components/ui/button";
import { Card, CardDescription, CardTitle } from "@/components/ui/card";

const SECTIONS: ExportSection[] = [
  "profile",
  "plans",
  "sessions",
  "metrics",
  "supplements",
];

function downloadJson(payload: unknown, filename: string) {
  const blob = new Blob([JSON.stringify(payload, null, 2)], {
    type: "application/json",
  });
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

export function AiPrepareClient() {
  const t = useTranslations("ai");
  const [pending, setPending] = useState<ExportSection | null>(null);
  const [error, setError] = useState<string | null>(null);

  const prepare = async (section: ExportSection) => {
    setPending(section);
    setError(null);
    try {
      const body = await fetchSectionExport(section);
      const iso = new Date().toISOString().slice(0, 10);
      downloadJson(body, `workouthub-${section}-${iso}.json`);
    } catch {
      setError(t("error"));
    } finally {
      setPending(null);
    }
  };

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-semibold tracking-tight">{t("title")}</h1>

      <Card className="space-y-3" data-testid="ai-cheatsheet">
        <CardTitle className="text-lg">{t("cheatsheetTitle")}</CardTitle>
        <CardDescription>{t("cheatsheetIntro")}</CardDescription>
        <ol className="list-decimal space-y-1 pl-5 text-sm">
          <li>{t("step1")}</li>
          <li>{t("step2")}</li>
          <li>{t("step3")}</li>
          <li>{t("step4")}</li>
        </ol>
        <p className="rounded-md bg-muted p-3 text-xs font-mono whitespace-pre-wrap">
          {t("examplePrompt")}
        </p>
      </Card>

      <Card className="space-y-3" data-testid="ai-section-picker">
        <CardTitle className="text-lg">{t("pickerTitle")}</CardTitle>
        <CardDescription>{t("pickerDescription")}</CardDescription>
        <div className="flex flex-wrap gap-2">
          {SECTIONS.map((section) => (
            <Button
              key={section}
              variant="outline"
              size="sm"
              onClick={() => prepare(section)}
              disabled={pending !== null}
              data-testid={`ai-section-${section}`}
            >
              {pending === section
                ? t("pending")
                : t(`section.${section}`)}
            </Button>
          ))}
        </div>
        {error && (
          <p role="alert" className="text-sm text-destructive">
            {error}
          </p>
        )}
      </Card>

      <Card className="space-y-3">
        <CardTitle className="text-lg">{t("applyTitle")}</CardTitle>
        <CardDescription>{t("applyDescription")}</CardDescription>
        <Button asChild variant="default">
          <Link href="/export/ai/apply">{t("applyButton")}</Link>
        </Button>
      </Card>
    </div>
  );
}
