"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { useMemo, useState } from "react";
import {
  CartesianGrid,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import {
  deleteMetric,
  fetchMetrics,
  upsertMetric,
  type UpsertBodyMetricPayload,
} from "@/lib/api/endpoints";
import type { BodyMetric } from "@/lib/api/schemas";
import { Button } from "@/components/ui/button";
import { Card, CardDescription, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

type Range = "week" | "month" | "all";

export function MetricsClient() {
  const t = useTranslations("metrics");
  const qc = useQueryClient();

  const metricsQuery = useQuery({
    queryKey: ["metrics"],
    queryFn: fetchMetrics,
  });

  const [range, setRange] = useState<Range>("month");
  const [form, setForm] = useState({
    recordedDate: new Date().toISOString().slice(0, 10),
    weightKg: "",
    bodyFatPercent: "",
    waistCm: "",
    notes: "",
  });

  const saveMutation = useMutation({
    mutationFn: (payload: UpsertBodyMetricPayload) => upsertMetric(payload),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["metrics"] });
      setForm((prev) => ({
        ...prev,
        weightKg: "",
        bodyFatPercent: "",
        waistCm: "",
        notes: "",
      }));
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => deleteMetric(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["metrics"] }),
  });

  const metrics: BodyMetric[] = metricsQuery.data ?? [];
  const chartData = useMemo(() => buildChartData(metrics, range), [metrics, range]);

  const submit = () => {
    const payload: UpsertBodyMetricPayload = { recordedDate: form.recordedDate };
    if (form.weightKg) payload.weightKg = Number(form.weightKg);
    if (form.bodyFatPercent) payload.bodyFatPercent = Number(form.bodyFatPercent);
    if (form.waistCm) payload.waistCm = Number(form.waistCm);
    if (form.notes) payload.notes = form.notes;
    saveMutation.mutate(payload);
  };

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-semibold tracking-tight">{t("title")}</h1>

      <Card className="space-y-3">
        <CardTitle className="text-lg">{t("chartTitle")}</CardTitle>
        <div className="flex gap-2">
          {(["week", "month", "all"] as Range[]).map((r) => (
            <Button
              key={r}
              size="sm"
              variant={r === range ? "default" : "outline"}
              onClick={() => setRange(r)}
              aria-pressed={r === range}
            >
              {t(`range.${r}` as "range.week")}
            </Button>
          ))}
        </div>
        <div className="h-56 w-full" data-testid="weight-chart">
          {chartData.length === 0 ? (
            <p className="flex h-full items-center justify-center text-sm text-muted-foreground">
              {t("empty")}
            </p>
          ) : (
            <ResponsiveContainer width="100%" height="100%">
              <LineChart data={chartData}>
                <CartesianGrid strokeDasharray="3 3" className="stroke-muted" />
                <XAxis dataKey="date" />
                <YAxis domain={["auto", "auto"]} />
                <Tooltip />
                <Line
                  type="monotone"
                  dataKey="weightKg"
                  stroke="currentColor"
                  strokeWidth={2}
                  dot={false}
                  className="text-primary"
                />
              </LineChart>
            </ResponsiveContainer>
          )}
        </div>
      </Card>

      <Card className="space-y-3">
        <CardTitle className="text-lg">{t("recordedDate")}</CardTitle>
        <div className="grid grid-cols-2 gap-3">
          <div className="space-y-1">
            <Label htmlFor="recordedDate">{t("recordedDate")}</Label>
            <Input
              id="recordedDate"
              type="date"
              value={form.recordedDate}
              onChange={(e) =>
                setForm((p) => ({ ...p, recordedDate: e.target.value }))
              }
            />
          </div>
          <div className="space-y-1">
            <Label htmlFor="weightKg">{t("weightKg")}</Label>
            <Input
              id="weightKg"
              type="number"
              inputMode="decimal"
              step="0.1"
              value={form.weightKg}
              onChange={(e) =>
                setForm((p) => ({ ...p, weightKg: e.target.value }))
              }
            />
          </div>
          <div className="space-y-1">
            <Label htmlFor="bodyFatPercent">{t("bodyFatPercent")}</Label>
            <Input
              id="bodyFatPercent"
              type="number"
              inputMode="decimal"
              step="0.1"
              value={form.bodyFatPercent}
              onChange={(e) =>
                setForm((p) => ({ ...p, bodyFatPercent: e.target.value }))
              }
            />
          </div>
          <div className="space-y-1">
            <Label htmlFor="waistCm">{t("waistCm")}</Label>
            <Input
              id="waistCm"
              type="number"
              inputMode="decimal"
              step="0.1"
              value={form.waistCm}
              onChange={(e) =>
                setForm((p) => ({ ...p, waistCm: e.target.value }))
              }
            />
          </div>
          <div className="col-span-2 space-y-1">
            <Label htmlFor="notes">{t("notes")}</Label>
            <Input
              id="notes"
              value={form.notes}
              onChange={(e) => setForm((p) => ({ ...p, notes: e.target.value }))}
            />
          </div>
        </div>
        <Button onClick={submit} disabled={saveMutation.isPending}>
          {saveMutation.isPending ? t("saving") : t("saveButton")}
        </Button>
      </Card>

      {metricsQuery.isLoading ? (
        <p className="text-muted-foreground">{t("loading")}</p>
      ) : metrics.length === 0 ? (
        <Card>
          <CardDescription>{t("empty")}</CardDescription>
        </Card>
      ) : (
        <ul className="space-y-2">
          {metrics.map((m) => (
            <li
              key={m.id}
              className="flex items-center gap-3 rounded-md border border-border px-3 py-2 text-sm"
            >
              <div className="flex-1">
                <p className="font-medium">{m.recordedDate}</p>
                <p className="text-muted-foreground">
                  {m.weightKg != null ? `${m.weightKg}kg` : "-"}
                  {m.bodyFatPercent != null ? ` · ${m.bodyFatPercent}%` : ""}
                  {m.notes ? ` · ${m.notes}` : ""}
                </p>
              </div>
              <Button
                size="sm"
                variant="ghost"
                onClick={() => {
                  if (window.confirm(t("deleteConfirm"))) {
                    deleteMutation.mutate(m.id);
                  }
                }}
                disabled={deleteMutation.isPending}
              >
                {t("deleteAction")}
              </Button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

function buildChartData(metrics: BodyMetric[], range: Range) {
  const filtered = metrics.filter((m) => m.weightKg != null);
  const sorted = [...filtered].sort((a, b) =>
    a.recordedDate.localeCompare(b.recordedDate)
  );
  const now = Date.now();
  const cutoff =
    range === "week"
      ? now - 7 * 24 * 60 * 60 * 1000
      : range === "month"
        ? now - 30 * 24 * 60 * 60 * 1000
        : 0;
  return sorted
    .filter((m) => new Date(m.recordedDate).getTime() >= cutoff)
    .map((m) => ({ date: m.recordedDate, weightKg: Number(m.weightKg) }));
}
