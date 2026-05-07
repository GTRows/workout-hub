"use client";

import { useQuery } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import {
  fetchActiveWorkoutPlan,
  fetchSessionHistory,
} from "@/lib/api/endpoints";
import { Card, CardDescription, CardTitle } from "@/components/ui/card";
import { getCurrentIsoWeekRange, isInRange } from "@/lib/time/iso-week";

export function WeeklySummaryCard() {
  const t = useTranslations("dashboard");
  const planQuery = useQuery({
    queryKey: ["workout-plans", "active"],
    queryFn: fetchActiveWorkoutPlan,
  });
  const historyQuery = useQuery({
    queryKey: ["sessions", "history", { page: 0, size: 50 }],
    queryFn: () => fetchSessionHistory(0, 50),
  });

  if (historyQuery.isLoading) {
    return (
      <Card>
        <CardDescription>{t("loading")}</CardDescription>
      </Card>
    );
  }

  if (historyQuery.isError) return null;

  const range = getCurrentIsoWeekRange();
  const entries = (historyQuery.data?.content ?? []).filter((s) =>
    isInRange(s.startedAt, range)
  );
  const total = entries.length;
  const completed = entries.filter((s) => s.finished).length;
  const target = planQuery.data?.days.length ?? null;

  if (total === 0 && target === null) {
    return (
      <Card>
        <CardDescription>{t("weeklySummaryEmpty")}</CardDescription>
      </Card>
    );
  }

  return (
    <Card className="space-y-2">
      <div className="space-y-1">
        <CardDescription>{t("weeklySummaryTitle")}</CardDescription>
        <CardTitle>
          {t("weeklySummaryCount", { completed, total })}
        </CardTitle>
        {target !== null && (
          <CardDescription>
            {t("weeklySummaryTarget", { actual: completed, target })}
          </CardDescription>
        )}
      </div>
    </Card>
  );
}
