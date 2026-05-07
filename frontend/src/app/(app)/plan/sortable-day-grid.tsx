"use client";

import {
  DndContext,
  KeyboardSensor,
  PointerSensor,
  closestCenter,
  useSensor,
  useSensors,
  type DragEndEvent,
} from "@dnd-kit/core";
import {
  SortableContext,
  rectSortingStrategy,
  sortableKeyboardCoordinates,
  useSortable,
} from "@dnd-kit/sortable";
import { CSS } from "@dnd-kit/utilities";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import type { CSSProperties } from "react";
import { useState } from "react";
import {
  updateWorkoutDay,
  type UpdateWorkoutDayPayload,
} from "@/lib/api/endpoints";
import { ApiError } from "@/lib/api/client";
import type { WorkoutDay, WorkoutPlan } from "@/lib/api/schemas";
import { DayCard } from "./day-card";

export type DayDragOutcome =
  | { kind: "noop" }
  | { kind: "collision" }
  | { kind: "move"; activeDayId: string; targetDow: number };

export function computeDayDragOutcome(
  activeId: string,
  overId: string,
  days: ReadonlyArray<WorkoutDay | null>
): DayDragOutcome {
  if (activeId === overId) return { kind: "noop" };
  const activeDay = days.find(
    (d): d is WorkoutDay => !!d && d.id === activeId
  );
  if (!activeDay) return { kind: "noop" };
  if (overId.startsWith("empty-")) {
    const dow = Number(overId.slice("empty-".length));
    if (!Number.isFinite(dow) || dow < 1 || dow > 7) return { kind: "noop" };
    return { kind: "move", activeDayId: activeDay.id, targetDow: dow };
  }
  // overId is a real day's id -> the target slot is occupied
  const targetDay = days.find((d): d is WorkoutDay => !!d && d.id === overId);
  if (targetDay) return { kind: "collision" };
  return { kind: "noop" };
}

export function SortableDayGrid({ plan }: { plan: WorkoutPlan }) {
  const t = useTranslations("plan");
  const qc = useQueryClient();
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const days: ReadonlyArray<WorkoutDay | null> = Array.from(
    { length: 7 },
    (_, i) => i + 1
  ).map((dow) => plan.days.find((d) => d.dayOfWeek === dow) ?? null);

  const moveMutation = useMutation({
    mutationFn: ({
      dayId,
      payload,
    }: {
      dayId: string;
      payload: UpdateWorkoutDayPayload;
    }) => updateWorkoutDay(plan.id, dayId, payload),
    onSuccess: () => {
      setErrorMessage(null);
      qc.invalidateQueries({ queryKey: ["workout-plans"] });
    },
    onError: (err: unknown) => {
      if (err instanceof ApiError && err.status === 409) {
        setErrorMessage(t("dayOfWeekTaken"));
      } else {
        setErrorMessage(t("dayMoveError"));
      }
    },
  });

  const sensors = useSensors(
    useSensor(PointerSensor, { activationConstraint: { distance: 5 } }),
    useSensor(KeyboardSensor, {
      coordinateGetter: sortableKeyboardCoordinates,
    })
  );

  const itemIds: string[] = days.map((d, i) => d?.id ?? `empty-${i + 1}`);

  const handleDragEnd = (event: DragEndEvent) => {
    const { active, over } = event;
    if (!over) return;
    const outcome = computeDayDragOutcome(
      String(active.id),
      String(over.id),
      days
    );
    if (outcome.kind === "noop") return;
    if (outcome.kind === "collision") {
      setErrorMessage(t("dayOfWeekTaken"));
      return;
    }
    setErrorMessage(null);
    moveMutation.mutate({
      dayId: outcome.activeDayId,
      payload: { dayOfWeek: outcome.targetDow },
    });
  };

  return (
    <div className="space-y-3" data-testid="sortable-day-grid">
      {errorMessage && (
        <p className="text-xs text-destructive" data-testid="day-grid-error">
          {errorMessage}
        </p>
      )}
      <DndContext
        sensors={sensors}
        collisionDetection={closestCenter}
        onDragEnd={handleDragEnd}
      >
        <SortableContext items={itemIds} strategy={rectSortingStrategy}>
          <ul className="space-y-3">
            {days.map((day, i) => (
              <li key={day?.id ?? `empty-${i + 1}`}>
                <SortableDayCardSlot
                  plan={plan}
                  day={day}
                  dayOfWeek={i + 1}
                  dragHandleLabel={t("dayDragHandle")}
                />
              </li>
            ))}
          </ul>
        </SortableContext>
      </DndContext>
    </div>
  );
}

function SortableDayCardSlot({
  plan,
  day,
  dayOfWeek,
  dragHandleLabel,
}: {
  plan: WorkoutPlan;
  day: WorkoutDay | null;
  dayOfWeek: number;
  dragHandleLabel: string;
}) {
  const id = day?.id ?? `empty-${dayOfWeek}`;
  const {
    attributes,
    listeners,
    setNodeRef,
    transform,
    transition,
    isDragging,
  } = useSortable({ id, disabled: !day });

  const style: CSSProperties = {
    transform: CSS.Transform.toString(transform),
    transition,
    opacity: isDragging ? 0.6 : 1,
  };

  return (
    <div
      ref={setNodeRef}
      style={style}
      className="flex items-start gap-2"
      data-testid={`sortable-slot-${dayOfWeek}`}
    >
      {day ? (
        <button
          type="button"
          {...attributes}
          {...listeners}
          className="mt-2 cursor-grab text-muted-foreground hover:text-foreground"
          aria-label={dragHandleLabel}
          data-testid={`day-drag-handle-${day.id}`}
        >
          ::
        </button>
      ) : (
        <span className="mt-2 w-4" aria-hidden="true" />
      )}
      <div className="flex-1">
        <DayCard plan={plan} day={day} dayOfWeek={dayOfWeek} />
      </div>
    </div>
  );
}
