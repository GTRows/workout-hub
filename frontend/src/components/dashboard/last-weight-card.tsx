"use client";

import { useQuery } from "@tanstack/react-query";
import { useLocale, useTranslations } from "next-intl";
import Link from "next/link";
import { fetchMetrics } from "@/lib/api/endpoints";
import { Button } from "@/components/ui/button";
import { Card, CardDescription, CardTitle } from "@/components/ui/card";
import { formatRelativeDays } from "@/lib/time/relative-date";

export function LastWeightCard() {
  const t = useTranslations("dashboard");
  const locale = useLocale();
  const metricsQuery = useQuery({
    queryKey: ["metrics", "list"],
    queryFn: fetchMetrics,
  });

  if (metricsQuery.isLoading) {
    return (
      <Card>
        <CardDescription>{t("loading")}</CardDescription>
      </Card>
    );
  }

  if (metricsQuery.isError) return null;

  const sorted = [...(metricsQuery.data ?? [])].sort((a, b) =>
    a.recordedDate < b.recordedDate
      ? 1
      : a.recordedDate > b.recordedDate
        ? -1
        : 0
  );
  const latest = sorted.find((m) => m.weightKg != null);

  if (!latest || latest.weightKg == null) {
    return (
      <Card className="space-y-3">
        <div className="space-y-1">
          <CardDescription>{t("lastWeightTitle")}</CardDescription>
          <CardTitle>{t("lastWeightEmpty")}</CardTitle>
        </div>
        <Button asChild variant="outline" size="sm">
          <Link href="/metrics">{t("lastWeightAddCta")}</Link>
        </Button>
      </Card>
    );
  }

  const rel = formatRelativeDays(latest.recordedDate);
  let dateLabel: string;
  if (rel.kind === "today") {
    dateLabel = t("lastWeightToday");
  } else if (rel.kind === "daysAgo") {
    dateLabel = t("lastWeightDaysAgo", { days: rel.days });
  } else {
    const formatted = new Intl.DateTimeFormat(locale, {
      dateStyle: "medium",
    }).format(new Date(rel.iso));
    dateLabel = t("lastWeightAbsolute", { date: formatted });
  }

  return (
    <Card className="space-y-2">
      <div className="space-y-1">
        <CardDescription>{t("lastWeightTitle")}</CardDescription>
        <CardTitle>
          {t("lastWeightValue", { kg: latest.weightKg })}
        </CardTitle>
        <CardDescription>{dateLabel}</CardDescription>
      </div>
    </Card>
  );
}
