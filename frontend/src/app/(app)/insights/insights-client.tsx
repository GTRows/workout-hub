"use client";

import { useQuery } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { useState } from "react";
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
  fetchOneRepMax,
  fetchPersonalRecords,
  fetchStreak,
  fetchWeeklyVolume,
} from "@/lib/api/endpoints";
import { Card, CardDescription, CardTitle } from "@/components/ui/card";
import { Heatmap } from "@/components/heatmap";
import { Label } from "@/components/ui/label";

export function InsightsClient() {
  const t = useTranslations("insights");

  const volumeQuery = useQuery({
    queryKey: ["analytics", "volume", 12],
    queryFn: () => fetchWeeklyVolume(12),
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
    queryKey: ["analytics", "heatmap", 12],
    queryFn: () => fetchHeatmap(12),
  });

  const [selectedExerciseId, setSelectedExerciseId] = useState<string>("");
  const effectiveExerciseId =
    selectedExerciseId || prsQuery.data?.[0]?.exerciseId || "";

  const oneRmQuery = useQuery({
    queryKey: ["analytics", "one-rm", effectiveExerciseId],
    queryFn: () => fetchOneRepMax(effectiveExerciseId),
    enabled: effectiveExerciseId.length > 0,
  });

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-semibold tracking-tight">{t("title")}</h1>

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
        {volumeQuery.isLoading ? (
          <p className="text-muted-foreground">{t("loading")}</p>
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
                <Tooltip />
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
                <Tooltip />
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
    </div>
  );
}
