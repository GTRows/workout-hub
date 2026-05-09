"use client";

import { useQuery } from "@tanstack/react-query";
import { useLocale, useTranslations } from "next-intl";
import Link from "next/link";
import { fetchLastPerformance } from "@/lib/api/endpoints";
import type { LastPerformance, SessionSet } from "@/lib/api/schemas";
import { Card, CardDescription, CardTitle } from "@/components/ui/card";
import { formatRelativeDays } from "@/lib/time/relative-date";

function pickBestPrSet(perf: LastPerformance | null): SessionSet | null {
  if (!perf) return null;
  const prSets = perf.sets.filter((s) => s.newPr === true);
  if (prSets.length === 0) return null;
  return prSets.reduce<SessionSet>((acc, s) => {
    const accWeight = acc.weightKg ?? -1;
    const sWeight = s.weightKg ?? -1;
    return sWeight > accWeight ? s : acc;
  }, prSets[0]);
}

export function ExercisePersonalRecordCard({
  exerciseId,
}: {
  exerciseId: string;
}) {
  const t = useTranslations("exercises");
  const locale = useLocale();

  const query = useQuery({
    queryKey: ["last-performance", exerciseId],
    queryFn: () => fetchLastPerformance(exerciseId),
  });

  if (query.isLoading) {
    return (
      <Card className="space-y-2" data-testid="exercise-pr-card">
        <CardTitle className="text-base">{t("personalRecordTitle")}</CardTitle>
        <CardDescription>{t("loading")}</CardDescription>
      </Card>
    );
  }

  const perf: LastPerformance | null = query.data ?? null;
  const bestSet = pickBestPrSet(perf);

  if (query.isError || !bestSet || !perf) {
    return (
      <Card className="space-y-2" data-testid="exercise-pr-card">
        <CardTitle className="text-base">{t("personalRecordTitle")}</CardTitle>
        <CardDescription>{t("personalRecordEmpty")}</CardDescription>
        <Link
          href="/dashboard"
          className="text-sm text-primary hover:underline"
          data-testid="exercise-pr-empty-cta"
        >
          {t("personalRecordEmptyCta")}
        </Link>
      </Card>
    );
  }

  const relative = formatRelativeDays(perf.startedAt);
  const relativeLine =
    relative.kind === "today"
      ? t("personalRecordAchievedToday")
      : relative.kind === "daysAgo"
        ? t("personalRecordAchievedDaysAgo", { days: relative.days })
        : t("personalRecordAchievedAbsolute", {
            date: new Intl.DateTimeFormat(locale, {
              dateStyle: "medium",
            }).format(new Date(relative.iso)),
          });

  return (
    <Card className="space-y-2" data-testid="exercise-pr-card">
      <CardTitle className="text-base">{t("personalRecordTitle")}</CardTitle>
      <CardDescription>
        {t("personalRecordValue", {
          weight: bestSet.weightKg ?? 0,
          reps: bestSet.repsDone,
        })}
      </CardDescription>
      <CardDescription>{relativeLine}</CardDescription>
    </Card>
  );
}
