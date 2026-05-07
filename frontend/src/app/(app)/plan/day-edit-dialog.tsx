"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  workoutFocusValues,
  type WorkoutDay,
  type WorkoutFocus,
} from "@/lib/api/schemas";

export type DayEditPayload = {
  dayOfWeek: number;
  name: string;
  focus: WorkoutFocus;
  estimatedDurationMin: number | null;
};

type Props =
  | {
      mode: "create";
      defaultDayOfWeek: number;
      onSubmit: (payload: DayEditPayload) => void;
      onCancel: () => void;
      pending?: boolean;
      errorMessage?: string | null;
    }
  | {
      mode: "edit";
      day: WorkoutDay;
      onSubmit: (payload: DayEditPayload) => void;
      onCancel: () => void;
      pending?: boolean;
      errorMessage?: string | null;
    };

export function DayEditDialog(props: Props) {
  const t = useTranslations("plan");

  const initialName = props.mode === "edit" ? props.day.name : "";
  const initialDayOfWeek =
    props.mode === "edit" ? props.day.dayOfWeek : props.defaultDayOfWeek;
  const initialFocus: WorkoutFocus =
    props.mode === "edit" ? props.day.focus : "PUSH";
  const initialDuration =
    props.mode === "edit"
      ? props.day.estimatedDurationMin?.toString() ?? ""
      : "";

  const [name, setName] = useState(initialName);
  const [dayOfWeek, setDayOfWeek] = useState<number>(initialDayOfWeek);
  const [focus, setFocus] = useState<WorkoutFocus>(initialFocus);
  const [estimatedDurationMin, setEstimatedDurationMin] =
    useState<string>(initialDuration);
  const [error, setError] = useState<string | null>(null);

  const pending = props.pending ?? false;

  const handleSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const trimmed = name.trim();
    if (!trimmed) {
      setError(t("nameRequired"));
      return;
    }
    let durationValue: number | null = null;
    if (estimatedDurationMin.trim() !== "") {
      const parsed = Number(estimatedDurationMin);
      if (!Number.isFinite(parsed) || parsed < 5 || parsed > 600) {
        setError(t("nameRequired"));
        return;
      }
      durationValue = parsed;
    }
    setError(null);
    props.onSubmit({
      dayOfWeek,
      name: trimmed,
      focus,
      estimatedDurationMin: durationValue,
    });
  };

  return (
    <form
      onSubmit={handleSubmit}
      className="space-y-2 rounded-md border border-border bg-muted/30 p-3"
      data-testid="day-edit-dialog"
    >
      <h3 className="text-sm font-medium">
        {props.mode === "create"
          ? t("dayDialogCreateTitle")
          : t("dayDialogEditTitle")}
      </h3>

      <div>
        <Label htmlFor="day-name">{t("dayNameLabel")}</Label>
        <Input
          id="day-name"
          value={name}
          onChange={(e) => setName(e.target.value)}
          autoFocus
          disabled={pending}
        />
      </div>

      <div>
        <Label htmlFor="day-of-week">{t("dayOfWeekLabel")}</Label>
        <select
          id="day-of-week"
          className="h-9 w-full rounded-md border border-input bg-background px-3 text-sm"
          value={dayOfWeek}
          onChange={(e) => setDayOfWeek(Number(e.target.value))}
          disabled={pending}
        >
          {[1, 2, 3, 4, 5, 6, 7].map((dow) => (
            <option key={dow} value={dow}>
              {t(`weekday.${dow}` as `weekday.1`)}
            </option>
          ))}
        </select>
      </div>

      <div>
        <Label htmlFor="day-focus">{t("focusLabel")}</Label>
        <select
          id="day-focus"
          className="h-9 w-full rounded-md border border-input bg-background px-3 text-sm"
          value={focus}
          onChange={(e) => setFocus(e.target.value as WorkoutFocus)}
          disabled={pending}
        >
          {workoutFocusValues.map((value) => (
            <option key={value} value={value}>
              {t(`focus.${value}` as `focus.PUSH`)}
            </option>
          ))}
        </select>
      </div>

      <div>
        <Label htmlFor="day-duration">{t("estimatedDurationMinLabel")}</Label>
        <Input
          id="day-duration"
          type="number"
          inputMode="numeric"
          min={5}
          max={600}
          value={estimatedDurationMin}
          onChange={(e) => setEstimatedDurationMin(e.target.value)}
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
