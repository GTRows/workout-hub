"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { useLocale, useTranslations } from "next-intl";
import { useMemo, useState } from "react";
import {
  Bar,
  BarChart,
  CartesianGrid,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import {
  fetchHeatmap,
  fetchMetrics,
  fetchOneRepMax,
  fetchPersonalRecords,
  fetchStreak,
  fetchWeeklyVolume,
} from "@/lib/api/endpoints";
import type { BodyMetric } from "@/lib/api/schemas";
import { Card, CardDescription, CardTitle } from "@/components/ui/card";
import { Heatmap } from "@/components/heatmap";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { RouteSkeleton } from "@/components/skeletons/route-skeleton";
import { pickLocaleField } from "@/lib/locale";

const VOLUME_RANGE_TO_WEEKS = {
  fourWeeks: 4,
  twelveWeeks: 12,
  twentySixWeeks: 26,
} as const;
type VolumeRange = keyof typeof VOLUME_RANGE_TO_WEEKS;
const VOLUME_RANGE_KEYS: VolumeRange[] = [
  "fourWeeks",
  "twelveWeeks",
  "twentySixWeeks",
];

const WEIGHT_WINDOW_TO_DAYS = {
  seven: 7,
  thirty: 30,
  all: null,
} as const;
type WeightRange = keyof typeof WEIGHT_WINDOW_TO_DAYS;
const WEIGHT_RANGE_KEYS: WeightRange[] = ["seven", "thirty", "all"];

function buildWeightWindow(metrics: BodyMetric[], daysBack: number | null) {
  const filtered = metrics.filter((m) => m.weightKg != null);
  const sorted = [...filtered].sort((a, b) =>
    a.recordedDate.localeCompare(b.recordedDate)
  );
  if (daysBack == null) {
    return sorted.map((m) => ({
      date: m.recordedDate,
      weightKg: Number(m.weightKg),
    }));
  }
  const cutoff = Date.now() - daysBack * 24 * 60 * 60 * 1000;
  return sorted
    .filter((m) => new Date(m.recordedDate).getTime() >= cutoff)
    .map((m) => ({ date: m.recordedDate, weightKg: Number(m.weightKg) }));
}

type ChartTooltipProps = {
  active?: boolean;
  label?: string | number;
  payload?: { value?: unknown }[];
};

function renderChartTooltip(
  { active, label, payload }: ChartTooltipProps,
  locale: string,
  valueLabel: string
) {
  if (!active || !payload || payload.length === 0) return null;
  const raw = payload[0]?.value;
  const num = typeof raw === "number" ? raw : Number(raw);
  const formatted = Number.isFinite(num)
    ? new Intl.NumberFormat(locale).format(num)
    : String(raw ?? "");
  return (
    <div
      className="rounded-md border border-border bg-background px-2 py-1 text-xs shadow"
      data-testid="insights-tooltip"
    >
      <p className="font-medium">{label ?? ""}</p>
      <p className="text-muted-foreground">
        {valueLabel}: {formatted}
      </p>
    </div>
  );
}

export function InsightsClient() {
  const t = useTranslations("insights");
  const tPrs = useTranslations("prs");
  const locale = useLocale();

  const [volumeRange, setVolumeRange] = useState<VolumeRange>("twelveWeeks");
  const volumeWeeks = VOLUME_RANGE_TO_WEEKS[volumeRange];

  const [weightRange, setWeightRange] = useState<WeightRange>("thirty");

  const metricsQuery = useQuery({
    queryKey: ["metrics"],
    queryFn: fetchMetrics,
  });

  const weightChartData = useMemo(
    () =>
      buildWeightWindow(
        metricsQuery.data ?? [],
        WEIGHT_WINDOW_TO_DAYS[weightRange]
      ),
    [metricsQuery.data, weightRange]
  );

  const weightCurrent =
    weightChartData.length > 0
      ? weightChartData[weightChartData.length - 1]!.weightKg
      : null;
  const weightDelta =
    weightChartData.length >= 2
      ? weightChartData[weightChartData.length - 1]!.weightKg -
        weightChartData[0]!.weightKg
      : null;

  const volumeQuery = useQuery({
    queryKey: ["analytics", "volume", volumeWeeks],
    queryFn: () => fetchWeeklyVolume(volumeWeeks),
  });

  const prsQuery = useQuery({
    queryKey: ["analytics", "prs"],
    queryFn: fetchPersonalRecords,
  });

  const streakQuery = useQuery({
    queryKey: ["analytics", "streak"],
    queryFn: fetchStreak,
  });

  const heatmapQuery = useQuery({
    queryKey: ["analytics", "heatmap", volumeWeeks],
    queryFn: () => fetchHeatmap(volumeWeeks),
  });

  const [selectedExerciseId, setSelectedExerciseId] = useState<string>("");
  const effectiveExerciseId =
    selectedExerciseId || prsQuery.data?.[0]?.exerciseId || "";

  const oneRmQuery = useQuery({
    queryKey: ["analytics", "one-rm", effectiveExerciseId],
    queryFn: () => fetchOneRepMax(effectiveExerciseId),
    enabled: effectiveExerciseId.length > 0,
  });

  const prTeaser = useMemo(() => {
    const all = prsQuery.data ?? [];
    const sorted = [...all].sort((a, b) =>
      b.achievedAt.localeCompare(a.achievedAt)
    );
    return sorted.slice(0, 3);
  }, [prsQuery.data]);

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-semibold tracking-tight">{t("title")}</h1>

      <Card className="space-y-3" data-testid="weight-trend-card">
        <CardTitle>{t("weightTitle")}</CardTitle>
        <CardDescription>{t("weightDescription")}</CardDescription>
        <div className="flex flex-wrap gap-2" data-testid="weight-trend-range">
          {WEIGHT_RANGE_KEYS.map((r) => (
            <Button
              key={r}
              size="sm"
              variant={r === weightRange ? "default" : "outline"}
              onClick={() => setWeightRange(r)}
              aria-pressed={r === weightRange}
            >
              {t(`weightRange.${r}` as "weightRange.seven")}
            </Button>
          ))}
        </div>
        {weightCurrent != null ? (
          <p className="text-sm" data-testid="weight-current">
            {t("weightCurrent", { value: weightCurrent })}
          </p>
        ) : null}
        {weightDelta != null ? (
          <p
            className="text-sm text-muted-foreground"
            data-testid="weight-delta"
          >
            {t("weightDelta", {
              value:
                weightDelta > 0
                  ? `+${weightDelta.toFixed(1)}`
                  : weightDelta.toFixed(1),
            })}
          </p>
        ) : (
          <p
            className="text-sm text-muted-foreground"
            data-testid="weight-delta-empty"
          >
            {t("weightDeltaEmpty")}
          </p>
        )}
        {weightChartData.length >= 2 ? (
          <div
            className="h-24 w-full"
            role="img"
            aria-label={t("weightTitle")}
            data-testid="weight-sparkline"
          >
            <ResponsiveContainer width="100%" height="100%">
              <LineChart data={weightChartData}>
                <Line
                  type="monotone"
                  dataKey="weightKg"
                  stroke="#0ea5e9"
                  strokeWidth={2}
                  dot={false}
                />
              </LineChart>
            </ResponsiveContainer>
          </div>
        ) : null}
      </Card>

      <Card className="space-y-3">
        <CardTitle>{t("streakTitle")}</CardTitle>
        <div className="flex flex-wrap gap-2">
          <span
            data-testid="streak-current"
            className="rounded-full bg-primary/10 px-3 py-1 text-sm font-medium text-primary"
          >
            {t("streakCurrent", {
              days: streakQuery.data?.currentStreakDays ?? 0,
            })}
          </span>
          <span
            data-testid="streak-longest"
            className="rounded-full bg-muted px-3 py-1 text-sm font-medium text-muted-foreground"
          >
            {t("streakLongest", {
              days: streakQuery.data?.longestStreakDays ?? 0,
            })}
          </span>
        </div>
        {heatmapQuery.data && heatmapQuery.data.length > 0 ? (
          <Heatmap days={heatmapQuery.data} />
        ) : (
          <p className="text-muted-foreground">{t("loading")}</p>
        )}
      </Card>

      <Card className="space-y-3">
        <CardTitle>{t("volumeTitle")}</CardTitle>
        <CardDescription>{t("volumeDescription")}</CardDescription>
        <div className="flex flex-wrap gap-2" data-testid="insights-volume-range">
          {VOLUME_RANGE_KEYS.map((r) => (
            <Button
              key={r}
              size="sm"
              variant={r === volumeRange ? "default" : "outline"}
              onClick={() => setVolumeRange(r)}
              aria-pressed={r === volumeRange}
            >
              {t(`range.${r}` as "range.fourWeeks")}
            </Button>
          ))}
        </div>
        {volumeQuery.isLoading ? (
          <RouteSkeleton variant="insights" />
        ) : (
          <div
            className="h-56 w-full"
            role="img"
            aria-label={t("volumeTitle")}
            data-testid="volume-chart"
          >
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={volumeQuery.data ?? []}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="weekStart" tick={{ fontSize: 10 }} />
                <YAxis tick={{ fontSize: 10 }} />
                <Tooltip
                  content={(props) =>
                    renderChartTooltip(
                      props as ChartTooltipProps,
                      locale,
                      t("volumeTooltipLabel")
                    )
                  }
                />
                <Bar
                  dataKey="totalVolumeKg"
                  fill="#0ea5e9"
                  name={t("volumeTitle")}
                />
              </BarChart>
            </ResponsiveContainer>
          </div>
        )}
      </Card>

      <Card className="space-y-3">
        <CardTitle>{t("oneRmTitle")}</CardTitle>
        <CardDescription>{t("oneRmDescription")}</CardDescription>
        {prsQuery.data && prsQuery.data.length > 0 ? (
          <div className="space-y-1">
            <Label htmlFor="exercise-select">{t("exerciseLabel")}</Label>
            <select
              id="exercise-select"
              className="h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm"
              value={effectiveExerciseId}
              onChange={(e) => setSelectedExerciseId(e.target.value)}
            >
              {prsQuery.data.map((p) => (
                <option key={p.exerciseId} value={p.exerciseId}>
                  {p.exerciseNameTr} / {p.exerciseNameEn}
                </option>
              ))}
            </select>
          </div>
        ) : null}

        {oneRmQuery.data && oneRmQuery.data.length > 0 ? (
          <div
            className="h-56 w-full"
            role="img"
            aria-label={t("oneRmTitle")}
            data-testid="one-rm-chart"
          >
            <ResponsiveContainer width="100%" height="100%">
              <LineChart data={oneRmQuery.data}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="date" tick={{ fontSize: 10 }} />
                <YAxis tick={{ fontSize: 10 }} domain={["auto", "auto"]} />
                <Tooltip
                  content={(props) =>
                    renderChartTooltip(
                      props as ChartTooltipProps,
                      locale,
                      t("oneRmTooltipLabel")
                    )
                  }
                />
                <Line
                  type="monotone"
                  dataKey="estimatedOneRmKg"
                  stroke="#0ea5e9"
                  strokeWidth={2}
                  dot={false}
                />
              </LineChart>
            </ResponsiveContainer>
          </div>
        ) : (
          <p className="text-muted-foreground">{t("oneRmEmpty")}</p>
        )}
      </Card>

      <Card className="space-y-3" data-testid="pr-teaser-card">
        <div className="flex items-center justify-between">
          <CardTitle>{t("prTeaserTitle")}</CardTitle>
          <Link
            href="/prs"
            className="text-xs text-primary hover:underline"
            data-testid="pr-teaser-view-all"
          >
            {t("prTeaserViewAll")}
          </Link>
        </div>
        {prTeaser.length === 0 ? (
          <p className="text-sm text-muted-foreground">{tPrs("empty")}</p>
        ) : (
          <ul className="space-y-1">
            {prTeaser.map((pr) => (
              <li
                key={pr.exerciseId}
                className="flex items-center justify-between text-sm"
                data-testid={`pr-teaser-row-${pr.exerciseId}`}
              >
                <span className="font-medium">
                  {pickLocaleField(locale, pr.exerciseNameTr, pr.exerciseNameEn)}
                </span>
                <span className="text-muted-foreground">
                  {pr.weightKg ?? 0} kg x {pr.repsDone} ({pr.achievedAt})
                </span>
              </li>
            ))}
          </ul>
        )}
      </Card>
    </div>
  );
}
