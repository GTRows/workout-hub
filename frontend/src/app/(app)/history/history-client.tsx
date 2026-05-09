"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useLocale, useTranslations } from "next-intl";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useMemo, useState } from "react";
import {
  fetchSession,
  fetchSessionHistory,
  startSession,
} from "@/lib/api/endpoints";
import {
  ApiErrorCode,
  apiErrorCodeMessageKey,
  isApiErrorWithCode,
} from "@/lib/api/api-error-codes";
import type { SessionDetail, SessionSummary } from "@/lib/api/schemas";
import { Button } from "@/components/ui/button";
import { Card, CardDescription, CardTitle } from "@/components/ui/card";
import { PrToast } from "@/components/pr-toast";
import { RouteSkeleton } from "@/components/skeletons/route-skeleton";
import { cn } from "@/lib/utils";

type MonthKey = { year: number; monthIndex: number };

export function HistoryClient() {
  const t = useTranslations("history");
  const tFull = useTranslations();
  const locale = useLocale();
  const router = useRouter();
  const qc = useQueryClient();
  const [month, setMonth] = useState<MonthKey>(() => {
    const now = new Date();
    return { year: now.getFullYear(), monthIndex: now.getMonth() };
  });
  const [selectedIso, setSelectedIso] = useState<string | null>(null);
  const [apiErrorToast, setApiErrorToast] = useState<string | null>(null);

  const historyQuery = useQuery({
    queryKey: ["sessions", "history"],
    queryFn: () => fetchSessionHistory(0, 200),
  });

  const repeatMutation = useMutation({
    mutationFn: (workoutDayId: string) => startSession(workoutDayId),
    onSuccess: (session: SessionDetail) => {
      qc.setQueryData(["sessions", "active"], session);
      router.push(`/session/${session.id}`);
    },
    onError: (err: unknown) => {
      if (isApiErrorWithCode(err, ApiErrorCode.SESSION_ALREADY_ACTIVE)) {
        setApiErrorToast(
          tFull(apiErrorCodeMessageKey(ApiErrorCode.SESSION_ALREADY_ACTIVE))
        );
        void qc.invalidateQueries({ queryKey: ["sessions", "active"] });
        return;
      }
      setApiErrorToast(tFull(apiErrorCodeMessageKey(undefined)));
    },
  });

  const grid = useMemo(
    () => buildMonthGrid(month.year, month.monthIndex),
    [month]
  );
  const sessionsByDate = useMemo(() => {
    const map = new Map<string, SessionSummary>();
    for (const s of historyQuery.data?.content ?? []) {
      if (!s.finished) continue;
      map.set(isoDate(new Date(s.startedAt)), s);
    }
    return map;
  }, [historyQuery.data]);

  const selectedSummary = selectedIso ? sessionsByDate.get(selectedIso) : null;

  const monthLabel = new Date(month.year, month.monthIndex, 1).toLocaleDateString(
    locale,
    { year: "numeric", month: "long" }
  );

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-semibold tracking-tight">{t("title")}</h1>

      <div className="flex items-center justify-between">
        <Button
          variant="outline"
          size="sm"
          onClick={() => setMonth(addMonths(month, -1))}
          aria-label={t("prevMonth")}
        >
          {t("prevMonth")}
        </Button>
        <span className="text-sm font-medium">{monthLabel}</span>
        <Button
          variant="outline"
          size="sm"
          onClick={() => setMonth(addMonths(month, 1))}
          aria-label={t("nextMonth")}
        >
          {t("nextMonth")}
        </Button>
      </div>

      <div className="grid grid-cols-7 gap-1 text-center text-xs">
        {(["mon", "tue", "wed", "thu", "fri", "sat", "sun"] as const).map(
          (key) => (
            <div key={key} className="py-1 text-muted-foreground">
              {t(`weekday.${key}` as `weekday.mon`)}
            </div>
          )
        )}
        {grid.map((date) => {
          const iso = isoDate(date);
          const inMonth = date.getMonth() === month.monthIndex;
          const hasSession = sessionsByDate.has(iso);
          const isSelected = iso === selectedIso;
          return (
            <button
              key={iso}
              type="button"
              aria-label={iso}
              aria-pressed={isSelected}
              onClick={() => setSelectedIso(iso)}
              className={cn(
                "flex aspect-square items-center justify-center rounded-md border border-transparent text-sm transition",
                inMonth ? "text-foreground" : "text-muted-foreground/50",
                hasSession &&
                  "border-primary bg-primary/10 font-semibold text-primary",
                isSelected && "ring-2 ring-primary"
              )}
            >
              {date.getDate()}
            </button>
          );
        })}
      </div>

      {selectedSummary ? (
        <SelectedSessionCard
          summary={selectedSummary}
          onRepeat={(dayId) => repeatMutation.mutate(dayId)}
          isRepeating={repeatMutation.isPending}
        />
      ) : selectedIso ? (
        <Card>
          <CardDescription>{t("noSession")}</CardDescription>
        </Card>
      ) : historyQuery.isLoading ? (
        <RouteSkeleton variant="history" />
      ) : (
        <Card>
          <CardDescription>{t("selectDayHint")}</CardDescription>
        </Card>
      )}

      {apiErrorToast ? (
        <PrToast
          message={apiErrorToast}
          onDismiss={() => setApiErrorToast(null)}
        />
      ) : null}
    </div>
  );
}

function SelectedSessionCard({
  summary,
  onRepeat,
  isRepeating,
}: {
  summary: SessionSummary;
  onRepeat: (workoutDayId: string) => void;
  isRepeating: boolean;
}) {
  const t = useTranslations("history");
  const detailQuery = useQuery({
    queryKey: ["sessions", summary.id],
    queryFn: () => fetchSession(summary.id),
  });
  const repeatableDayId = summary.workoutDayId;

  return (
    <Card className="space-y-2">
      <CardTitle>{t("sessionTitle")}</CardTitle>
      <CardDescription>
        {t("sessionStartedAt", { time: formatTime(summary.startedAt) })}
      </CardDescription>
      {summary.endedAt && (
        <CardDescription>
          {t("sessionFinishedAt", { time: formatTime(summary.endedAt) })}
        </CardDescription>
      )}
      <CardDescription>
        {t("setCount", { count: summary.setCount })}
      </CardDescription>
      {detailQuery.data && (
        <ul className="space-y-1 text-sm">
          {detailQuery.data.sets.map((s) => (
            <li key={s.id}>
              {s.exerciseNameTr ?? s.exerciseNameEn} {" - "}
              {s.setNumber}. {s.repsDone}
              {s.weightKg != null ? ` x ${s.weightKg}kg` : ""}
            </li>
          ))}
        </ul>
      )}
      <div className="flex flex-wrap gap-2">
        <Button asChild variant="outline" size="sm">
          <Link href={`/session/${summary.id}`}>{t("viewFullDetail")}</Link>
        </Button>
        {repeatableDayId ? (
          <Button
            variant="default"
            size="sm"
            disabled={isRepeating}
            onClick={() => onRepeat(repeatableDayId)}
          >
            {isRepeating ? t("repeatWorkoutPending") : t("repeatWorkout")}
          </Button>
        ) : null}
      </div>
    </Card>
  );
}

function buildMonthGrid(year: number, monthIndex: number): Date[] {
  const firstOfMonth = new Date(year, monthIndex, 1);
  // Monday-first week. JS getDay returns 0=Sun..6=Sat -> remap to 0=Mon..6=Sun.
  const firstDayMondayOffset = (firstOfMonth.getDay() + 6) % 7;
  const start = new Date(year, monthIndex, 1 - firstDayMondayOffset);
  const grid: Date[] = [];
  for (let i = 0; i < 42; i++) {
    grid.push(
      new Date(start.getFullYear(), start.getMonth(), start.getDate() + i)
    );
  }
  return grid;
}

function addMonths({ year, monthIndex }: MonthKey, delta: number): MonthKey {
  const d = new Date(year, monthIndex + delta, 1);
  return { year: d.getFullYear(), monthIndex: d.getMonth() };
}

function isoDate(d: Date): string {
  const pad = (n: number) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

function formatTime(iso: string): string {
  try {
    return new Date(iso).toLocaleTimeString(undefined, {
      hour: "2-digit",
      minute: "2-digit",
    });
  } catch {
    return iso;
  }
}
