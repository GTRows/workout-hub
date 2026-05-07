"use client";

import { useQuery } from "@tanstack/react-query";
import { useLocale, useTranslations } from "next-intl";
import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { fetchExercises } from "@/lib/api/endpoints";
import type { WorkoutDayExercise } from "@/lib/api/schemas";
import { pickLocaleField } from "@/lib/locale";

export type ExerciseEditPayload = {
  exerciseId?: string;
  targetSets: number;
  targetRepsMin: number | null;
  targetRepsMax: number | null;
  targetWeightKg: number | null;
  restSeconds: number | null;
  notes: string | null;
};

type Props =
  | {
      mode: "add";
      onSubmit: (payload: ExerciseEditPayload) => void;
      onCancel: () => void;
      pending?: boolean;
      errorMessage?: string | null;
    }
  | {
      mode: "edit";
      item: WorkoutDayExercise;
      onSubmit: (payload: ExerciseEditPayload) => void;
      onCancel: () => void;
      pending?: boolean;
      errorMessage?: string | null;
    };

function parseOptionalNumber(
  raw: string,
  min: number,
  max: number
): { ok: true; value: number | null } | { ok: false } {
  const trimmed = raw.trim();
  if (trimmed === "") return { ok: true, value: null };
  const parsed = Number(trimmed);
  if (!Number.isFinite(parsed) || parsed < min || parsed > max) {
    return { ok: false };
  }
  return { ok: true, value: parsed };
}

export function ExerciseEditDialog(props: Props) {
  const t = useTranslations("plan");
  const locale = useLocale();

  const initialSets =
    props.mode === "edit" ? String(props.item.targetSets) : "3";
  const initialRepsMin =
    props.mode === "edit" ? props.item.targetRepsMin?.toString() ?? "" : "";
  const initialRepsMax =
    props.mode === "edit" ? props.item.targetRepsMax?.toString() ?? "" : "";
  const initialWeightKg =
    props.mode === "edit" ? props.item.targetWeightKg?.toString() ?? "" : "";
  const initialRestSeconds =
    props.mode === "edit" ? props.item.restSeconds?.toString() ?? "" : "";
  const initialNotes =
    props.mode === "edit" ? props.item.notes ?? "" : "";

  const [exerciseId, setExerciseId] = useState<string>("");
  const [targetSets, setTargetSets] = useState<string>(initialSets);
  const [targetRepsMin, setTargetRepsMin] = useState<string>(initialRepsMin);
  const [targetRepsMax, setTargetRepsMax] = useState<string>(initialRepsMax);
  const [targetWeightKg, setTargetWeightKg] = useState<string>(initialWeightKg);
  const [restSeconds, setRestSeconds] = useState<string>(initialRestSeconds);
  const [notes, setNotes] = useState<string>(initialNotes);
  const [error, setError] = useState<string | null>(null);

  const pending = props.pending ?? false;

  const exerciseListQuery = useQuery({
    queryKey: ["exercises", "list", "all"],
    queryFn: () => fetchExercises({ page: 0, size: 200 }),
    staleTime: 5 * 60 * 1000,
    enabled: props.mode === "add",
  });

  const handleSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();

    if (props.mode === "add" && exerciseId === "") {
      setError(t("exerciseRequired"));
      return;
    }

    const setsParsed = Number(targetSets);
    if (!Number.isFinite(setsParsed) || setsParsed < 1 || setsParsed > 50) {
      setError(t("targetSetsRequired"));
      return;
    }

    const repsMin = parseOptionalNumber(targetRepsMin, 1, 200);
    if (!repsMin.ok) {
      setError(t("mutationError"));
      return;
    }
    const repsMax = parseOptionalNumber(targetRepsMax, 1, 200);
    if (!repsMax.ok) {
      setError(t("mutationError"));
      return;
    }
    const weight = parseOptionalNumber(targetWeightKg, 0, 999.99);
    if (!weight.ok) {
      setError(t("mutationError"));
      return;
    }
    const rest = parseOptionalNumber(restSeconds, 0, 3600);
    if (!rest.ok) {
      setError(t("mutationError"));
      return;
    }

    if (
      repsMin.value !== null &&
      repsMax.value !== null &&
      repsMin.value > repsMax.value
    ) {
      setError(t("repsRangeInvalid"));
      return;
    }

    const trimmedNotes = notes.trim();
    const notesValue = trimmedNotes === "" ? null : trimmedNotes;

    setError(null);

    const basePayload: ExerciseEditPayload = {
      targetSets: setsParsed,
      targetRepsMin: repsMin.value,
      targetRepsMax: repsMax.value,
      targetWeightKg: weight.value,
      restSeconds: rest.value,
      notes: notesValue,
    };

    if (props.mode === "add") {
      props.onSubmit({ ...basePayload, exerciseId });
    } else {
      props.onSubmit(basePayload);
    }
  };

  return (
    <form
      onSubmit={handleSubmit}
      className="space-y-2 rounded-md border border-border bg-muted/30 p-3"
      data-testid="exercise-edit-dialog"
    >
      <h3 className="text-sm font-medium">
        {props.mode === "add"
          ? t("exerciseDialogAddTitle")
          : t("exerciseDialogEditTitle")}
      </h3>

      {props.mode === "add" && (
        <div>
          <Label htmlFor="exercise-pick">{t("exerciseLabel")}</Label>
          {exerciseListQuery.isLoading ? (
            <p className="text-xs text-muted-foreground">
              {t("loadingExercises")}
            </p>
          ) : (
            <select
              id="exercise-pick"
              className="h-9 w-full rounded-md border border-input bg-background px-3 text-sm"
              value={exerciseId}
              onChange={(e) => setExerciseId(e.target.value)}
              disabled={pending}
              data-testid="exercise-pick"
            >
              <option value="">{t("exercisePlaceholder")}</option>
              {exerciseListQuery.data?.content.map((ex) => (
                <option key={ex.id} value={ex.id}>
                  {pickLocaleField(locale, ex.nameTr, ex.nameEn)}
                </option>
              ))}
            </select>
          )}
        </div>
      )}

      <div>
        <Label htmlFor="target-sets">{t("targetSetsLabel")}</Label>
        <Input
          id="target-sets"
          type="number"
          inputMode="numeric"
          min={1}
          max={50}
          value={targetSets}
          onChange={(e) => setTargetSets(e.target.value)}
          disabled={pending}
        />
      </div>

      <div className="flex gap-2">
        <div className="flex-1">
          <Label htmlFor="target-reps-min">{t("targetRepsMinLabel")}</Label>
          <Input
            id="target-reps-min"
            type="number"
            inputMode="numeric"
            min={1}
            max={200}
            value={targetRepsMin}
            onChange={(e) => setTargetRepsMin(e.target.value)}
            disabled={pending}
          />
        </div>
        <div className="flex-1">
          <Label htmlFor="target-reps-max">{t("targetRepsMaxLabel")}</Label>
          <Input
            id="target-reps-max"
            type="number"
            inputMode="numeric"
            min={1}
            max={200}
            value={targetRepsMax}
            onChange={(e) => setTargetRepsMax(e.target.value)}
            disabled={pending}
          />
        </div>
      </div>

      <div className="flex gap-2">
        <div className="flex-1">
          <Label htmlFor="target-weight">{t("targetWeightKgLabel")}</Label>
          <Input
            id="target-weight"
            type="number"
            inputMode="decimal"
            min={0}
            max={999.99}
            step={0.5}
            value={targetWeightKg}
            onChange={(e) => setTargetWeightKg(e.target.value)}
            disabled={pending}
          />
        </div>
        <div className="flex-1">
          <Label htmlFor="rest-seconds">{t("restSecondsLabel")}</Label>
          <Input
            id="rest-seconds"
            type="number"
            inputMode="numeric"
            min={0}
            max={3600}
            value={restSeconds}
            onChange={(e) => setRestSeconds(e.target.value)}
            disabled={pending}
          />
        </div>
      </div>

      <div>
        <Label htmlFor="notes">{t("notesLabel")}</Label>
        <Input
          id="notes"
          maxLength={1000}
          value={notes}
          onChange={(e) => setNotes(e.target.value)}
          disabled={pending}
        />
      </div>

      {error && <p className="text-xs text-destructive">{error}</p>}
      {props.errorMessage && (
        <p className="text-xs text-destructive">{props.errorMessage}</p>
      )}

      <div className="flex gap-2">
        <Button type="submit" size="sm" disabled={pending}>
          {t("dialogSave")}
        </Button>
        <Button
          type="button"
          variant="ghost"
          size="sm"
          onClick={props.onCancel}
          disabled={pending}
        >
          {t("dialogCancel")}
        </Button>
      </div>
    </form>
  );
}
