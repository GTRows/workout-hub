"use client";

import { useEffect, useState, useSyncExternalStore } from "react";
import { useLocale, useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { Card, CardDescription, CardTitle } from "@/components/ui/card";
import {
  clearHistory,
  getHistory,
  subscribeHistory,
  type HistoryEntry,
} from "./export-history";

function relativeTime(now: number, then: number, locale: string): string {
  const diffMs = then - now;
  const diffSec = Math.round(diffMs / 1000);
  const abs = Math.abs(diffSec);
  const rtf = new Intl.RelativeTimeFormat(locale, { numeric: "auto" });
  if (abs < 60) return rtf.format(diffSec, "second");
  if (abs < 3600) return rtf.format(Math.round(diffSec / 60), "minute");
  if (abs < 86400) return rtf.format(Math.round(diffSec / 3600), "hour");
  return rtf.format(Math.round(diffSec / 86400), "day");
}

const EMPTY_SNAPSHOT: HistoryEntry[] = [];

export function ExportHistoryCard() {
  const t = useTranslations("export");
  const locale = useLocale();

  const entries = useSyncExternalStore<HistoryEntry[]>(
    subscribeHistory,
    getHistory,
    () => EMPTY_SNAPSHOT,
  );

  const [now, setNow] = useState(() => Date.now());
  useEffect(() => {
    const id = window.setInterval(() => setNow(Date.now()), 30_000);
    return () => window.clearInterval(id);
  }, []);

  return (
    <Card className="space-y-3" data-testid="history-card">
      <div className="space-y-1">
        <CardTitle className="text-lg">{t("historyTitle")}</CardTitle>
        <CardDescription>{t("historyDescription")}</CardDescription>
      </div>
      {entries.length === 0 ? (
        <p
          className="text-sm text-muted-foreground"
          data-testid="history-empty"
        >
          {t("historyEmpty")}
        </p>
      ) : (
        <ul className="space-y-2" data-testid="history-list">
          {entries.map((e) => (
            <li
              key={`${e.kind}-${e.filename}-${e.timestamp}`}
              className="flex items-center justify-between gap-3 text-sm"
            >
              <div className="space-y-0.5">
                <p className="font-medium">
                  {t(`historyKind.${e.kind}` as never)}
                  {e.sectionLabel ? `: ${e.sectionLabel}` : ""}
                </p>
                <p className="text-xs text-muted-foreground">{e.filename}</p>
              </div>
              <span className="text-xs text-muted-foreground">
                {relativeTime(now, e.timestamp, locale)}
              </span>
            </li>
          ))}
        </ul>
      )}
      <div>
        <Button
          variant="outline"
          size="sm"
          onClick={() => clearHistory()}
          disabled={entries.length === 0}
          data-testid="history-clear"
        >
          {t("historyClear")}
        </Button>
      </div>
    </Card>
  );
}
