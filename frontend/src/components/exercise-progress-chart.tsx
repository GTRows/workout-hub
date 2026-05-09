"use client";

import { useQuery } from "@tanstack/react-query";
import { useLocale, useTranslations } from "next-intl";
import { useMemo } from "react";
import {
  CartesianGrid,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { fetchExerciseProgress } from "@/lib/api/endpoints";
import type { ProgressPoint } from "@/lib/api/schemas";
import { Card, CardTitle } from "@/components/ui/card";

type ChartPoint = {
  x: string;
  oneRm: number;
  maxWeight: number;
  volume: number;
  sets: number;
  reps: number;
};

function buildChartData(points: ProgressPoint[]): ChartPoint[] {
  return [...points]
    .sort(
      (a, b) =>
        new Date(a.startedAt).getTime() - new Date(b.startedAt).getTime()
    )
    .map((p) => ({
      x: p.startedAt,
      oneRm: p.estimatedOneRmKg ?? 0,
      maxWeight: p.maxWeightKg ?? 0,
      volume: p.totalVolumeKg,
      sets: p.setCount,
      reps: p.topRepsDone,
    }));
}

type TooltipPayloadEntry = { payload?: ChartPoint };

type CustomTooltipProps = {
  active?: boolean;
  label?: string;
  payload?: TooltipPayloadEntry[];
};

export function ExerciseProgressChart({ exerciseId }: { exerciseId: string }) {
  const t = useTranslations("exercises");
  const locale = useLocale();

  const query = useQuery({
    queryKey: ["exercise-progress", exerciseId],
    queryFn: () => fetchExerciseProgress(exerciseId),
  });

  const data = useMemo<ChartPoint[]>(
    () => (query.data ? buildChartData(query.data) : []),
    [query.data]
  );

  if (query.isLoading) {
    return (
      <Card className="space-y-2" data-testid="exercise-progress-chart">
        <CardTitle className="text-base">{t("progressChartTitle")}</CardTitle>
        <p className="text-sm text-muted-foreground">
          {t("progressChartLoading")}
        </p>
      </Card>
    );
  }

  if (query.isError || data.length === 0) {
    return (
      <Card className="space-y-2" data-testid="exercise-progress-chart">
        <CardTitle className="text-base">{t("progressChartTitle")}</CardTitle>
        <p className="text-sm text-muted-foreground">
          {t("progressChartEmpty")}
        </p>
      </Card>
    );
  }

  const tickFormatter = (value: string): string =>
    new Intl.DateTimeFormat(locale, {
      month: "short",
      day: "numeric",
    }).format(new Date(value));

  const renderTooltip = ({ active, label, payload }: CustomTooltipProps) => {
    if (!active || !payload || payload.length === 0) return null;
    const point = payload[0]?.payload;
    if (!point) return null;
    const dateLabel = label
      ? new Intl.DateTimeFormat(locale, { dateStyle: "medium" }).format(
          new Date(label)
        )
      : "";
    return (
      <div
        className="rounded-md border border-border bg-background p-2 text-xs shadow-sm"
        data-testid="exercise-progress-chart-tooltip"
      >
        <p className="font-medium" data-testid="tooltip-date">
          {dateLabel}
        </p>
        <p>{t("progressChartTooltipOneRm", { value: point.oneRm })}</p>
        <p>{t("progressChartTooltipMaxWeight", { value: point.maxWeight })}</p>
        <p>{t("progressChartTooltipVolume", { value: point.volume })}</p>
        <p>{t("progressChartTooltipSets", { count: point.sets })}</p>
        <p>{t("progressChartTooltipReps", { count: point.reps })}</p>
      </div>
    );
  };

  return (
    <Card className="space-y-2" data-testid="exercise-progress-chart">
      <CardTitle className="text-base">{t("progressChartTitle")}</CardTitle>
      <div className="h-60 w-full">
        <ResponsiveContainer width="100%" height="100%">
          <LineChart data={data}>
            <CartesianGrid strokeDasharray="3 3" className="stroke-muted" />
            <XAxis dataKey="x" tickFormatter={tickFormatter} />
            <YAxis domain={["auto", "auto"]} />
            <Tooltip content={renderTooltip} />
            <Line
              type="monotone"
              dataKey="oneRm"
              stroke="currentColor"
              strokeWidth={2}
              dot={false}
              className="text-primary"
            />
          </LineChart>
        </ResponsiveContainer>
      </div>
    </Card>
  );
}
