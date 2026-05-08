"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import {
  addSet,
  fetchLastPerformance,
  fetchSession,
  fetchWorkoutDay,
  finishSession,
  type AddSetPayload,
} from "@/lib/api/endpoints";
import {
  ApiErrorCode,
  isApiError,
  isApiErrorWithCode,
} from "@/lib/api/api-error-codes";
import {
  drainForSession,
  enqueueSet,
  queuedCount,
  subscribeOnline,
  type PostOutcome,
} from "@/lib/offline/session-set-queue";
import type {
  SessionDetail,
  SessionSet,
  WorkoutDayExercise,
} from "@/lib/api/schemas";
import { Button } from "@/components/ui/button";
import { Card, CardDescription, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { notifyRestElapsed, useRestTimer } from "@/lib/push/rest-timer";
import { PrToast } from "@/components/pr-toast";
import { ExerciseDetailModal } from "./exercise-detail-modal";

class OfflineQueuedError extends Error {
  constructor() {
    super("offline-queued");
    this.name = "OfflineQueuedError";
  }
}

function makeDrainPostFn(sessionId: string) {
  return async (payload: AddSetPayload): Promise<PostOutcome> => {
    try {
      await addSet(sessionId, payload);
      return { ok: true };
    } catch (err) {
      // Permanent rejects (session finished elsewhere) come BEFORE the bare
      // 409 check because they may carry status 409 with a more specific
      // code; treating them as duplicates would silently drop them under
      // the wrong counter.
      if (
        isApiErrorWithCode(err, ApiErrorCode.SESSION_FINISHED) ||
        isApiErrorWithCode(err, ApiErrorCode.SESSION_ALREADY_FINISHED)
      ) {
        return {
          ok: false,
          permanent: true,
          reason: err.code ?? "SESSION_FINISHED",
        };
      }
      if (isApiError(err) && err.status === 409) {
        return { ok: false, status: 409 };
      }
      throw err;
    }
  };
}

export function SessionClient({ sessionId }: { sessionId: string }) {
  const t = useTranslations("session");
  const router = useRouter();
  const qc = useQueryClient();

  const [queuedCountState, setQueuedCount] = useState(0);
  const [queuedToast, setQueuedToast] = useState<string | null>(null);
  const [drainedToast, setDrainedToast] = useState<string | null>(null);
  const [droppedToast, setDroppedToast] = useState<string | null>(null);

  const refreshQueued = useCallback(async () => {
    try {
      setQueuedCount(await queuedCount(sessionId));
    } catch {
      // IndexedDB unavailable (SSR or denied) - skip silently.
    }
  }, [sessionId]);

  const onOfflineQueued = useCallback(() => {
    setQueuedToast(t("queuedOffline"));
  }, [t]);

  const sessionQuery = useQuery({
    queryKey: ["sessions", sessionId],
    queryFn: () => fetchSession(sessionId),
  });
  const session = sessionQuery.data ?? null;

  const dayQuery = useQuery({
    queryKey: ["workout-days", session?.workoutDayId],
    queryFn: () =>
      session?.workoutDayId ? fetchWorkoutDay(session.workoutDayId) : null,
    enabled: !!session?.workoutDayId,
  });
  const day = dayQuery.data ?? null;

  const finishMutation = useMutation({
    mutationFn: () => finishSession(sessionId),
    onSuccess: (finished) => {
      qc.setQueryData(["sessions", sessionId], finished);
      qc.setQueryData(["sessions", "active"], null);
      router.push("/dashboard");
    },
  });

  useEffect(() => {
    let cancelled = false;
    const safeRunDrain = async () => {
      if (cancelled) return;
      try {
        const result = await drainForSession(
          sessionId,
          makeDrainPostFn(sessionId)
        );
        if (cancelled) return;
        if (result.drained > 0) {
          await qc.invalidateQueries({ queryKey: ["sessions", sessionId] });
          setDrainedToast(t("queuedDrained", { count: result.drained }));
        }
        if (result.permanentlyDropped > 0) {
          setDroppedToast(t("queuedDropped"));
        }
        setQueuedCount(result.remaining);
      } catch {
        // IndexedDB unavailable (SSR or denied) - skip silently.
      }
    };
    void safeRunDrain();
    const unsubscribe = subscribeOnline(() => {
      void safeRunDrain();
    });
    return () => {
      cancelled = true;
      unsubscribe();
    };
  }, [sessionId, qc, t]);

  if (sessionQuery.isLoading) {
    return <p className="text-muted-foreground">{t("loading")}</p>;
  }
  if (sessionQuery.isError || !session) {
    return <p className="text-destructive">{t("notFound")}</p>;
  }

  const setsByExerciseId = groupBy(session.sets, (s) => s.exerciseId ?? "?");
  const plannedExercises: WorkoutDayExercise[] = day?.exercises ?? [];
  const plannedExerciseIds = new Set(
    plannedExercises.map((p) => p.exerciseId).filter((id): id is string => !!id)
  );
  const adHocExerciseIds = Array.from(setsByExerciseId.keys()).filter(
    (id) => id !== "?" && !plannedExerciseIds.has(id)
  );

  return (
    <div className="space-y-4 pb-24">
      <Card className="space-y-1">
        <CardTitle>{day?.name ?? t("adHocTitle")}</CardTitle>
        <CardDescription>
          {t("startedAt", { time: formatTime(session.startedAt) })}
        </CardDescription>
        {session.finished && (
          <CardDescription className="font-medium text-primary">
            {t("finished")}
          </CardDescription>
        )}
        {queuedCountState > 0 && (
          <CardDescription className="font-medium text-amber-700 dark:text-amber-400">
            {t("queuedBadge", { count: queuedCountState })}
          </CardDescription>
        )}
      </Card>

      {plannedExercises.map((planItem) => (
        <ExerciseBlock
          key={planItem.id}
          sessionId={sessionId}
          planItem={planItem}
          existingSets={
            setsByExerciseId.get(planItem.exerciseId ?? "?") ?? []
          }
          locked={session.finished}
          onOfflineQueued={onOfflineQueued}
          refreshQueued={refreshQueued}
        />
      ))}

      {adHocExerciseIds.map((exId) => {
        const sets = setsByExerciseId.get(exId) ?? [];
        const first = sets[0];
        return (
          <AdHocBlock
            key={exId}
            name={first?.exerciseNameTr ?? first?.exerciseNameEn ?? "?"}
            existingSets={sets}
          />
        );
      })}

      <Button
        size="lg"
        variant="destructive"
        className="w-full"
        disabled={finishMutation.isPending || session.finished}
        onClick={() => {
          if (window.confirm(t("finishConfirm"))) {
            finishMutation.mutate();
          }
        }}
      >
        {t("finishButton")}
      </Button>

      {droppedToast ? (
        <PrToast
          message={droppedToast}
          onDismiss={() => setDroppedToast(null)}
        />
      ) : drainedToast ? (
        <PrToast
          message={drainedToast}
          onDismiss={() => setDrainedToast(null)}
        />
      ) : queuedToast ? (
        <PrToast
          message={queuedToast}
          onDismiss={() => setQueuedToast(null)}
        />
      ) : null}
    </div>
  );
}

function ExerciseBlock({
  sessionId,
  planItem,
  existingSets,
  locked,
  onOfflineQueued,
  refreshQueued,
}: {
  sessionId: string;
  planItem: WorkoutDayExercise;
  existingSets: SessionSet[];
  locked: boolean;
  onOfflineQueued: () => void;
  refreshQueued: () => void;
}) {
  const t = useTranslations("session");
  const qc = useQueryClient();
  const [reps, setReps] = useState("");
  const [weight, setWeight] = useState("");
  const [detailOpen, setDetailOpen] = useState(false);
  const [prCelebration, setPrCelebration] = useState<string | null>(null);
  const restTimer = useRestTimer(notifyRestElapsed);

  const lastPerfQuery = useQuery({
    queryKey: ["last-performance", planItem.exerciseId],
    queryFn: () =>
      planItem.exerciseId ? fetchLastPerformance(planItem.exerciseId) : null,
    enabled: !!planItem.exerciseId && !locked,
  });

  const mutation = useMutation({
    mutationFn: async (payload: AddSetPayload) => {
      if (typeof navigator !== "undefined" && navigator.onLine === false) {
        await enqueueSet(sessionId, payload);
        throw new OfflineQueuedError();
      }
      // Drain anything queued during a brief offline blip BEFORE shipping
      // the new submit, so FIFO ordering is preserved.
      try {
        await drainForSession(sessionId, makeDrainPostFn(sessionId));
      } catch {
        // Drain errors don't block the live submit.
      }
      return addSet(sessionId, payload);
    },
    onSuccess: (newSet) => {
      qc.setQueryData<SessionDetail | undefined>(
        ["sessions", sessionId],
        (prev) => (prev ? { ...prev, sets: [...prev.sets, newSet] } : prev)
      );
      setReps("");
      setWeight("");
      if (newSet.newPr) {
        setPrCelebration(
          planItem.exerciseNameTr ?? planItem.exerciseNameEn ?? "PR"
        );
      }
      if (planItem.restSeconds && planItem.restSeconds > 0) {
        restTimer.start(planItem.restSeconds);
      }
      void refreshQueued();
    },
    onError: (err) => {
      if (err instanceof OfflineQueuedError) {
        setReps("");
        setWeight("");
        onOfflineQueued();
        void refreshQueued();
      }
    },
  });

  const nextSetNumber = existingSets.length + 1;
  const canSubmit =
    !!planItem.exerciseId &&
    reps !== "" &&
    !mutation.isPending &&
    !locked;

  const submit = () => {
    if (!planItem.exerciseId) return;
    mutation.mutate({
      exerciseId: planItem.exerciseId,
      setNumber: nextSetNumber,
      repsDone: Number(reps),
      weightKg: weight === "" ? undefined : Number(weight),
      completed: true,
      clientSetId: crypto.randomUUID(),
    });
  };

  const targetText =
    planItem.targetRepsMin && planItem.targetRepsMax
      ? t("target", {
          sets: planItem.targetSets,
          min: planItem.targetRepsMin,
          max: planItem.targetRepsMax,
        })
      : t("targetSimple", { sets: planItem.targetSets });

  return (
    <Card className="space-y-4">
      <div className="space-y-1">
        <CardTitle>
          {planItem.exerciseId ? (
            <button
              type="button"
              onClick={() => setDetailOpen(true)}
              className="text-left hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary"
              data-testid={`open-detail-${planItem.id}`}
            >
              {planItem.exerciseNameTr ?? planItem.exerciseNameEn ?? "?"}
            </button>
          ) : (
            planItem.exerciseNameTr ?? planItem.exerciseNameEn ?? "?"
          )}
        </CardTitle>
        <CardDescription>{targetText}</CardDescription>
        <LastPerformanceChip data={lastPerfQuery.data ?? null} />
      </div>
      {detailOpen && planItem.exerciseId && (
        <ExerciseDetailModal
          exerciseId={planItem.exerciseId}
          onClose={() => setDetailOpen(false)}
        />
      )}
      {prCelebration && (
        <PrToast
          message={`New PR: ${prCelebration}`}
          onDismiss={() => setPrCelebration(null)}
        />
      )}

      {existingSets.length > 0 && (
        <ul className="space-y-1 text-sm">
          {existingSets.map((s) => (
            <li key={s.id}>{formatSetLine(s)}</li>
          ))}
        </ul>
      )}

      {restTimer.secondsRemaining !== null && (
        <p className="text-sm font-medium text-primary" data-testid="rest-timer">
          {t("restTimer", { seconds: restTimer.secondsRemaining })}
        </p>
      )}

      {!locked && (
        <div className="grid grid-cols-[1fr_1fr_auto] items-end gap-2">
          <div>
            <Label htmlFor={`reps-${planItem.id}`}>{t("reps")}</Label>
            <Input
              id={`reps-${planItem.id}`}
              type="number"
              inputMode="numeric"
              min={0}
              value={reps}
              onChange={(e) => setReps(e.target.value)}
            />
          </div>
          <div>
            <Label htmlFor={`weight-${planItem.id}`}>{t("weightKg")}</Label>
            <Input
              id={`weight-${planItem.id}`}
              type="number"
              inputMode="decimal"
              step="0.5"
              min={0}
              value={weight}
              onChange={(e) => setWeight(e.target.value)}
            />
          </div>
          <Button onClick={submit} disabled={!canSubmit}>
            {t("complete")}
          </Button>
        </div>
      )}
    </Card>
  );
}

function AdHocBlock({
  name,
  existingSets,
}: {
  name: string;
  existingSets: SessionSet[];
}) {
  return (
    <Card className="space-y-3">
      <CardTitle>{name}</CardTitle>
      <ul className="space-y-1 text-sm">
        {existingSets.map((s) => (
          <li key={s.id}>{formatSetLine(s)}</li>
        ))}
      </ul>
    </Card>
  );
}

function LastPerformanceChip({
  data,
}: {
  data: { sets: SessionSet[] } | null;
}) {
  const t = useTranslations("session");
  if (!data) {
    return (
      <p className="text-xs text-muted-foreground">
        {t("lastPerformanceNone")}
      </p>
    );
  }
  const summary = data.sets
    .map((s) =>
      s.weightKg != null
        ? `${s.repsDone}x${s.weightKg}`
        : String(s.repsDone)
    )
    .join(", ");
  return (
    <p className="text-xs text-muted-foreground">
      {t("lastPerformance")}: {summary}
    </p>
  );
}

function formatSetLine(s: SessionSet): string {
  if (s.weightKg != null) {
    return `${s.setNumber}. ${s.repsDone} x ${s.weightKg}kg`;
  }
  return `${s.setNumber}. ${s.repsDone} reps`;
}

function formatTime(iso: string): string {
  try {
    const d = new Date(iso);
    return d.toLocaleTimeString(undefined, { hour: "2-digit", minute: "2-digit" });
  } catch {
    return iso;
  }
}

function groupBy<T, K>(items: T[], keyFn: (t: T) => K): Map<K, T[]> {
  const out = new Map<K, T[]>();
  for (const item of items) {
    const key = keyFn(item);
    const existing = out.get(key);
    if (existing) existing.push(item);
    else out.set(key, [item]);
  }
  return out;
}
