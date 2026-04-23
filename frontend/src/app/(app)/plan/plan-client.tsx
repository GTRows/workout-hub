"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useLocale, useTranslations } from "next-intl";
import Link from "next/link";
import { useState } from "react";
import {
  fetchActiveWorkoutPlan,
  reorderDayExercises,
} from "@/lib/api/endpoints";
import type {
  WorkoutDay,
  WorkoutDayExercise,
  WorkoutPlan,
} from "@/lib/api/schemas";
import { pickLocaleField } from "@/lib/locale";
import { Button } from "@/components/ui/button";
import { Card, CardDescription, CardTitle } from "@/components/ui/card";
import { cn } from "@/lib/utils";

export function PlanClient() {
  const t = useTranslations("plan");
  const planQuery = useQuery({
    queryKey: ["workout-plans", "active"],
    queryFn: fetchActiveWorkoutPlan,
  });

  if (planQuery.isLoading) {
    return <p className="text-muted-foreground">{t("loading")}</p>;
  }
  if (!planQuery.data) {
    return (
      <Card>
        <CardDescription>{t("noPlan")}</CardDescription>
      </Card>
    );
  }

  const plan = planQuery.data;
  const days = Array.from({ length: 7 }, (_, i) => i + 1).map(
    (dow) => plan.days.find((d) => d.dayOfWeek === dow) ?? null
  );

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-semibold tracking-tight">{t("title")}</h1>
      {days.map((day, i) => (
        <DayCard key={day?.id ?? `empty-${i}`} plan={plan} day={day} dayOfWeek={i + 1} />
      ))}
    </div>
  );
}

function DayCard({
  plan,
  day,
  dayOfWeek,
}: {
  plan: WorkoutPlan;
  day: WorkoutDay | null;
  dayOfWeek: number;
}) {
  const t = useTranslations("plan");
  const locale = useLocale();
  const [expanded, setExpanded] = useState(false);

  const dayName = t(`weekday.${dayOfWeek}` as `weekday.1`);
  const title = day ? day.name : dayName;
  const subtitle = day
    ? `${dayName} · ${day.exercises.length}`
    : `${dayName}`;

  return (
    <Card
      className={cn("space-y-3", !day && "opacity-60")}
      aria-label={`day-${dayOfWeek}`}
    >
      <button
        type="button"
        className="flex w-full flex-col items-start text-left"
        onClick={() => day && setExpanded((v) => !v)}
        disabled={!day}
      >
        <CardTitle>{title}</CardTitle>
        <CardDescription>{subtitle}</CardDescription>
      </button>

      {day && expanded && (
        <DayExercises plan={plan} day={day} />
      )}
    </Card>
  );
}

function DayExercises({ plan, day }: { plan: WorkoutPlan; day: WorkoutDay }) {
  const t = useTranslations("plan");
  const locale = useLocale();
  const qc = useQueryClient();

  // Local optimistic order so the UI reflects the move immediately.
  const [localOrder, setLocalOrder] = useState<string[] | null>(null);

  const orderedItems: WorkoutDayExercise[] = (() => {
    const byId = new Map(day.exercises.map((e) => [e.id, e]));
    const ids = localOrder ?? day.exercises.map((e) => e.id);
    return ids
      .map((id) => byId.get(id))
      .filter((e): e is WorkoutDayExercise => !!e);
  })();

  const mutation = useMutation({
    mutationFn: (idsInOrder: string[]) =>
      reorderDayExercises(plan.id, day.id, idsInOrder),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["workout-plans", "active"] });
    },
    onError: () => {
      // Revert optimistic state on failure.
      setLocalOrder(null);
    },
  });

  if (orderedItems.length === 0) {
    return <p className="text-sm text-muted-foreground">{t("emptyDay")}</p>;
  }

  const move = (index: number, delta: -1 | 1) => {
    const target = index + delta;
    if (target < 0 || target >= orderedItems.length) return;
    const next = [...orderedItems];
    const [moved] = next.splice(index, 1);
    next.splice(target, 0, moved);
    const ids = next.map((e) => e.id);
    setLocalOrder(ids);
    mutation.mutate(ids);
  };

  return (
    <ul className="space-y-2">
      {orderedItems.map((item, index) => (
        <li
          key={item.id}
          className="flex items-center gap-2 rounded-md border border-border px-3 py-2"
        >
          <div className="flex-1">
            <Link
              href={`/exercises/${item.exerciseId}`}
              className="text-sm font-medium hover:underline"
            >
              {pickLocaleField(locale, item.exerciseNameTr, item.exerciseNameEn)}
            </Link>
            <p className="text-xs text-muted-foreground">
              {item.targetRepsMin && item.targetRepsMax
                ? t("setsReps", {
                    sets: item.targetSets,
                    min: item.targetRepsMin,
                    max: item.targetRepsMax,
                  })
                : t("setsOnly", { sets: item.targetSets })}
            </p>
          </div>
          <Button
            variant="outline"
            size="sm"
            aria-label={t("moveUp")}
            onClick={() => move(index, -1)}
            disabled={index === 0 || mutation.isPending}
          >
            {t("moveUp")}
          </Button>
          <Button
            variant="outline"
            size="sm"
            aria-label={t("moveDown")}
            onClick={() => move(index, 1)}
            disabled={
              index === orderedItems.length - 1 || mutation.isPending
            }
          >
            {t("moveDown")}
          </Button>
        </li>
      ))}
    </ul>
  );
}
