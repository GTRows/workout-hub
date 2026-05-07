"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { useState } from "react";
import {
  createWorkoutDay,
  deleteWorkoutDay,
  updateWorkoutDay,
  type CreateWorkoutDayPayload,
  type UpdateWorkoutDayPayload,
} from "@/lib/api/endpoints";
import type { WorkoutDay, WorkoutPlan } from "@/lib/api/schemas";
import { ApiError } from "@/lib/api/client";
import { Button } from "@/components/ui/button";
import { Card, CardDescription, CardTitle } from "@/components/ui/card";
import { cn } from "@/lib/utils";
import { DayEditDialog } from "./day-edit-dialog";
import { DayExercisesSection } from "./day-exercises-section";

export function DayCard({
  plan,
  day,
  dayOfWeek,
}: {
  plan: WorkoutPlan;
  day: WorkoutDay | null;
  dayOfWeek: number;
}) {
  const t = useTranslations("plan");
  const qc = useQueryClient();
  const [expanded, setExpanded] = useState(false);
  const [editing, setEditing] = useState(false);
  const [creating, setCreating] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const invalidateAll = () =>
    qc.invalidateQueries({ queryKey: ["workout-plans"] });

  const handleMutationError = (err: unknown) => {
    if (err instanceof ApiError && err.status === 409) {
      setErrorMessage(t("dayOfWeekTaken"));
    } else {
      setErrorMessage(t("mutationError"));
    }
  };

  const createMutation = useMutation({
    mutationFn: (payload: CreateWorkoutDayPayload) =>
      createWorkoutDay(plan.id, payload),
    onSuccess: () => {
      setCreating(false);
      setErrorMessage(null);
      invalidateAll();
    },
    onError: handleMutationError,
  });

  const updateMutation = useMutation({
    mutationFn: (payload: UpdateWorkoutDayPayload) => {
      if (!day) throw new Error("update without day");
      return updateWorkoutDay(plan.id, day.id, payload);
    },
    onSuccess: () => {
      setEditing(false);
      setErrorMessage(null);
      invalidateAll();
    },
    onError: handleMutationError,
  });

  const deleteMutation = useMutation({
    mutationFn: () => {
      if (!day) throw new Error("delete without day");
      return deleteWorkoutDay(plan.id, day.id);
    },
    onSuccess: () => {
      setErrorMessage(null);
      invalidateAll();
    },
    onError: handleMutationError,
  });

  const dayName = t(`weekday.${dayOfWeek}` as `weekday.1`);
  const title = day ? day.name : dayName;
  const subtitle = day ? `${dayName} · ${day.exercises.length}` : `${dayName}`;

  return (
    <Card
      className={cn("space-y-3", !day && "opacity-60")}
      aria-label={`day-${dayOfWeek}`}
      data-testid={`day-card-${dayOfWeek}`}
    >
      {creating ? (
        <DayEditDialog
          mode="create"
          defaultDayOfWeek={dayOfWeek}
          pending={createMutation.isPending}
          errorMessage={errorMessage}
          onSubmit={(payload) => createMutation.mutate(payload)}
          onCancel={() => {
            setCreating(false);
            setErrorMessage(null);
          }}
        />
      ) : editing && day ? (
        <DayEditDialog
          mode="edit"
          day={day}
          pending={updateMutation.isPending}
          errorMessage={errorMessage}
          onSubmit={(payload) => updateMutation.mutate(payload)}
          onCancel={() => {
            setEditing(false);
            setErrorMessage(null);
          }}
        />
      ) : (
        <>
          <button
            type="button"
            className="flex w-full flex-col items-start text-left"
            onClick={() => day && setExpanded((v) => !v)}
            disabled={!day}
          >
            <CardTitle>{title}</CardTitle>
            <CardDescription>{subtitle}</CardDescription>
          </button>

          {day ? (
            <div className="flex flex-wrap gap-2">
              <Button
                variant="ghost"
                size="sm"
                onClick={() => setEditing(true)}
                data-testid={`edit-day-${dayOfWeek}`}
              >
                {t("editDay")}
              </Button>
              <Button
                variant="ghost"
                size="sm"
                onClick={() => {
                  if (window.confirm(t("deleteDayConfirm"))) {
                    deleteMutation.mutate();
                  }
                }}
                disabled={deleteMutation.isPending}
                data-testid={`delete-day-${dayOfWeek}`}
              >
                {t("deleteDay")}
              </Button>
            </div>
          ) : (
            <Button
              variant="outline"
              size="sm"
              onClick={() => setCreating(true)}
              data-testid={`add-day-${dayOfWeek}`}
            >
              {t("addDay")}
            </Button>
          )}

          {errorMessage && !creating && !editing && (
            <p className="text-xs text-destructive">{errorMessage}</p>
          )}

          {day && expanded && <DayExercisesSection plan={plan} day={day} />}
        </>
      )}
    </Card>
  );
}
