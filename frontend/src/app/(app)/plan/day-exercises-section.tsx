"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useLocale, useTranslations } from "next-intl";
import { useState } from "react";
import {
  addDayExercise,
  removeDayExercise,
  updateDayExercise,
  type AddDayExercisePayload,
  type UpdateDayExercisePayload,
} from "@/lib/api/endpoints";
import type {
  WorkoutDay,
  WorkoutDayExercise,
  WorkoutPlan,
} from "@/lib/api/schemas";
import { pickLocaleField } from "@/lib/locale";
import { Button } from "@/components/ui/button";
import { DayExercises } from "./plan-client";
import {
  ExerciseEditDialog,
  type ExerciseEditPayload,
} from "./exercise-edit-dialog";

function stripExerciseId(payload: ExerciseEditPayload): UpdateDayExercisePayload {
  return {
    targetSets: payload.targetSets,
    targetRepsMin: payload.targetRepsMin,
    targetRepsMax: payload.targetRepsMax,
    targetWeightKg: payload.targetWeightKg,
    restSeconds: payload.restSeconds,
    notes: payload.notes,
  };
}

export function DayExercisesSection({
  plan,
  day,
}: {
  plan: WorkoutPlan;
  day: WorkoutDay;
}) {
  const t = useTranslations("plan");
  const locale = useLocale();
  const qc = useQueryClient();

  const [adding, setAdding] = useState(false);
  const [editingItemId, setEditingItemId] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const invalidateAll = () =>
    qc.invalidateQueries({ queryKey: ["workout-plans"] });

  const handleError = () => setErrorMessage(t("mutationError"));

  const addMutation = useMutation({
    mutationFn: (payload: AddDayExercisePayload) =>
      addDayExercise(plan.id, day.id, payload),
    onSuccess: () => {
      setAdding(false);
      setErrorMessage(null);
      invalidateAll();
    },
    onError: handleError,
  });

  const updateMutation = useMutation({
    mutationFn: ({
      itemId,
      payload,
    }: {
      itemId: string;
      payload: UpdateDayExercisePayload;
    }) => updateDayExercise(plan.id, day.id, itemId, payload),
    onSuccess: () => {
      setEditingItemId(null);
      setErrorMessage(null);
      invalidateAll();
    },
    onError: handleError,
  });

  const removeMutation = useMutation({
    mutationFn: (itemId: string) =>
      removeDayExercise(plan.id, day.id, itemId),
    onSuccess: () => {
      setErrorMessage(null);
      invalidateAll();
    },
    onError: handleError,
  });

  return (
    <div
      className="space-y-3"
      data-testid={`day-exercises-section-${day.id}`}
    >
      {adding ? (
        <ExerciseEditDialog
          mode="add"
          pending={addMutation.isPending}
          errorMessage={errorMessage}
          onSubmit={(payload) => {
            if (!payload.exerciseId) return;
            addMutation.mutate({
              exerciseId: payload.exerciseId,
              targetSets: payload.targetSets,
              targetRepsMin: payload.targetRepsMin,
              targetRepsMax: payload.targetRepsMax,
              targetWeightKg: payload.targetWeightKg,
              restSeconds: payload.restSeconds,
              notes: payload.notes,
            });
          }}
          onCancel={() => {
            setAdding(false);
            setErrorMessage(null);
          }}
        />
      ) : (
        <Button
          variant="outline"
          size="sm"
          onClick={() => setAdding(true)}
          data-testid={`add-exercise-${day.id}`}
        >
          {t("addExercise")}
        </Button>
      )}

      <DayExercises plan={plan} day={day} />

      {day.exercises.length > 0 && (
        <ul
          className="space-y-2"
          data-testid={`day-exercises-manage-${day.id}`}
        >
          {day.exercises.map((item) => (
            <li key={item.id}>
              {editingItemId === item.id ? (
                <ExerciseEditDialog
                  mode="edit"
                  item={item}
                  pending={updateMutation.isPending}
                  errorMessage={errorMessage}
                  onSubmit={(payload) => {
                    updateMutation.mutate({
                      itemId: item.id,
                      payload: stripExerciseId(payload),
                    });
                  }}
                  onCancel={() => {
                    setEditingItemId(null);
                    setErrorMessage(null);
                  }}
                />
              ) : (
                <ExerciseManageRow
                  item={item}
                  locale={locale}
                  editLabel={t("editExercise")}
                  removeLabel={t("removeExercise")}
                  removePending={removeMutation.isPending}
                  onEdit={() => setEditingItemId(item.id)}
                  onRemove={() => {
                    if (window.confirm(t("removeExerciseConfirm"))) {
                      removeMutation.mutate(item.id);
                    }
                  }}
                />
              )}
            </li>
          ))}
        </ul>
      )}

      {errorMessage && !adding && editingItemId === null && (
        <p className="text-xs text-destructive">{errorMessage}</p>
      )}
    </div>
  );
}

function ExerciseManageRow({
  item,
  locale,
  editLabel,
  removeLabel,
  removePending,
  onEdit,
  onRemove,
}: {
  item: WorkoutDayExercise;
  locale: string;
  editLabel: string;
  removeLabel: string;
  removePending: boolean;
  onEdit: () => void;
  onRemove: () => void;
}) {
  return (
    <div
      className="flex items-center justify-between gap-2 rounded-md border border-border bg-background px-3 py-2"
      data-testid={`exercise-manage-row-${item.id}`}
    >
      <span className="text-sm">
        {pickLocaleField(locale, item.exerciseNameTr, item.exerciseNameEn)}
      </span>
      <div className="flex gap-2">
        <Button
          variant="ghost"
          size="sm"
          onClick={onEdit}
          data-testid={`edit-exercise-${item.id}`}
        >
          {editLabel}
        </Button>
        <Button
          variant="ghost"
          size="sm"
          onClick={onRemove}
          disabled={removePending}
          data-testid={`remove-exercise-${item.id}`}
        >
          {removeLabel}
        </Button>
      </div>
    </div>
  );
}
