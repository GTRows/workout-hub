"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import {
  addSet,
  cancelRestTimer,
  deleteSet,
  fetchLastPerformance,
  fetchSession,
  fetchWorkoutDay,
  finishSession,
  updateSet,
  type AddSetPayload,
  type UpdateSetPayload,
} from "@/lib/api/endpoints";
import {
  ApiErrorCode,
  apiErrorCodeMessageKey,
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
  const tFull = useTranslations();
  const router = useRouter();
  const qc = useQueryClient();

  const [queuedCountState, setQueuedCount] = useState(0);
  const [queuedToast, setQueuedToast] = useState<string | null>(null);
  const [drainedToast, setDrainedToast] = useState<string | null>(null);
  const [droppedToast, setDroppedToast] = useState<string | null>(null);
  const [apiErrorToast, setApiErrorToast] = useState<string | null>(null);

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
      // Cancel any pending rest-timer schedule so a stale push is not
      // delivered after the session is over.
      cancelRestTimer(sessionId).catch(() => {});
      router.push("/dashboard");
    },
    onError: (err: unknown) => {
      if (
        isApiErrorWithCode(err, ApiErrorCode.SESSION_ALREADY_FINISHED) ||
        isApiErrorWithCode(err, ApiErrorCode.SESSION_FINISHED)
      ) {
        const code = isApiErrorWithCode(
          err,
          ApiErrorCode.SESSION_ALREADY_FINISHED
        )
          ? ApiErrorCode.SESSION_ALREADY_FINISHED
          : ApiErrorCode.SESSION_FINISHED;
        setApiErrorToast(tFull(apiErrorCodeMessageKey(code)));
        cancelRestTimer(sessionId).catch(() => {});
        router.push("/dashboard");
        return;
      }
      if (isApiError(err)) {
        setApiErrorToast(tFull(apiErrorCodeMessageKey(err.code)));
        return;
      }
      setApiErrorToast(tFull(apiErrorCodeMessageKey(undefined)));
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
          setApiErrorToast={setApiErrorToast}
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

      {apiErrorToast ? (
        <PrToast
          message={apiErrorToast}
          onDismiss={() => setApiErrorToast(null)}
        />
      ) : droppedToast ? (
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
  setApiErrorToast,
}: {
  sessionId: string;
  planItem: WorkoutDayExercise;
  existingSets: SessionSet[];
  locked: boolean;
  onOfflineQueued: () => void;
  refreshQueued: () => void;
  setApiErrorToast: (msg: string) => void;
}) {
  const t = useTranslations("session");
  const tFull = useTranslations();
  const qc = useQueryClient();
  const router = useRouter();
  const [reps, setReps] = useState("");
  const [weight, setWeight] = useState("");
  const [rpe, setRpe] = useState("");
  const [detailOpen, setDetailOpen] = useState(false);
  const [prCelebration, setPrCelebration] = useState<string | null>(null);
  const [editingSetId, setEditingSetId] = useState<string | null>(null);
  const restTimer = useRestTimer(notifyRestElapsed, {
    sessionId,
    title: t("restPushTitle"),
    body: (seconds: number) => t("restPushBody", { seconds }),
  });

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
      setRpe("");
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
    onError: (err: unknown) => {
      if (err instanceof OfflineQueuedError) {
        setReps("");
        setWeight("");
        setRpe("");
        onOfflineQueued();
        void refreshQueued();
        return;
      }
      if (isApiErrorWithCode(err, ApiErrorCode.SESSION_FINISHED)) {
        setApiErrorToast(
          tFull(apiErrorCodeMessageKey(ApiErrorCode.SESSION_FINISHED))
        );
        router.push("/dashboard");
        return;
      }
      if (isApiErrorWithCode(err, ApiErrorCode.SET_NUMBER_DUPLICATE)) {
        setApiErrorToast(
          tFull(apiErrorCodeMessageKey(ApiErrorCode.SET_NUMBER_DUPLICATE))
        );
        void qc.invalidateQueries({ queryKey: ["sessions", sessionId] });
        return;
      }
      if (isApiError(err)) {
        setApiErrorToast(tFull(apiErrorCodeMessageKey(err.code)));
        return;
      }
      setApiErrorToast(tFull(apiErrorCodeMessageKey(undefined)));
    },
  });

  const updateMutation = useMutation({
    mutationFn: async ({
      setId,
      payload,
    }: {
      setId: string;
      payload: UpdateSetPayload;
    }) => updateSet(sessionId, setId, payload),
    onSuccess: (updatedSet) => {
      qc.setQueryData<SessionDetail | undefined>(
        ["sessions", sessionId],
        (prev) =>
          prev
            ? {
                ...prev,
                sets: prev.sets.map((s) =>
                  s.id === updatedSet.id ? updatedSet : s
                ),
              }
            : prev
      );
      setEditingSetId(null);
    },
    onError: (err: unknown) => {
      if (isApiErrorWithCode(err, ApiErrorCode.SESSION_FINISHED)) {
        setApiErrorToast(
          tFull(apiErrorCodeMessageKey(ApiErrorCode.SESSION_FINISHED))
        );
        router.push("/dashboard");
        return;
      }
      if (isApiError(err) && err.status === 404) {
        void qc.invalidateQueries({ queryKey: ["sessions", sessionId] });
        setEditingSetId(null);
        return;
      }
      if (isApiError(err)) {
        setApiErrorToast(tFull(apiErrorCodeMessageKey(err.code)));
        return;
      }
      setApiErrorToast(tFull(apiErrorCodeMessageKey(undefined)));
    },
  });

  const deleteMutation = useMutation({
    mutationFn: async (setId: string) => {
      await deleteSet(sessionId, setId);
      return setId;
    },
    onSuccess: (deletedId) => {
      qc.setQueryData<SessionDetail | undefined>(
        ["sessions", sessionId],
        (prev) =>
          prev
            ? { ...prev, sets: prev.sets.filter((s) => s.id !== deletedId) }
            : prev
      );
    },
    onError: (err: unknown) => {
      if (isApiErrorWithCode(err, ApiErrorCode.SESSION_FINISHED)) {
        setApiErrorToast(
          tFull(apiErrorCodeMessageKey(ApiErrorCode.SESSION_FINISHED))
        );
        router.push("/dashboard");
        return;
      }
      if (isApiError(err) && err.status === 404) {
        void qc.invalidateQueries({ queryKey: ["sessions", sessionId] });
        return;
      }
      if (isApiError(err)) {
        setApiErrorToast(tFull(apiErrorCodeMessageKey(err.code)));
        return;
      }
      setApiErrorToast(tFull(apiErrorCodeMessageKey(undefined)));
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
      rpe: rpe === "" ? undefined : Number(rpe),
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
            <li
              key={s.id}
              className="flex items-center justify-between gap-2"
            >
              {editingSetId === s.id ? (
                <EditSetRow
                  set={s}
                  pending={updateMutation.isPending}
                  onSave={(payload) =>
                    updateMutation.mutate({ setId: s.id, payload })
                  }
                  onCancel={() => setEditingSetId(null)}
                />
              ) : (
                <>
                  <span>{formatSetLine(s)}</span>
                  {!locked && (
                    <span className="flex gap-1">
                      <Button
                        size="sm"
                        variant="ghost"
                        onClick={() => setEditingSetId(s.id)}
                        data-testid={`edit-set-${s.id}`}
                      >
                        {t("editAction")}
                      </Button>
                      <Button
                        size="sm"
                        variant="ghost"
                        onClick={() => {
                          if (window.confirm(t("deleteConfirm"))) {
                            deleteMutation.mutate(s.id);
                          }
                        }}
                        disabled={deleteMutation.isPending}
                        data-testid={`delete-set-${s.id}`}
                      >
                        {t("deleteAction")}
                      </Button>
                    </span>
                  )}
                </>
              )}
            </li>
          ))}
        </ul>
      )}

      {restTimer.secondsRemaining !== null && (
        <p className="text-sm font-medium text-primary" data-testid="rest-timer">
          {t("restTimer", { seconds: restTimer.secondsRemaining })}
        </p>
      )}

      {!locked && (
        <div className="grid grid-cols-[1fr_1fr_1fr_auto] items-end gap-2">
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
          <div>
            <Label htmlFor={`rpe-${planItem.id}`}>{t("rpe")}</Label>
            <Input
              id={`rpe-${planItem.id}`}
              type="number"
              inputMode="numeric"
              min={1}
              max={10}
              placeholder={t("rpeHint")}
              value={rpe}
              onChange={(e) => setRpe(e.target.value)}
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

function EditSetRow({
  set,
  pending,
  onSave,
  onCancel,
}: {
  set: SessionSet;
  pending: boolean;
  onSave: (payload: UpdateSetPayload) => void;
  onCancel: () => void;
}) {
  const t = useTranslations("session");
  const [reps, setReps] = useState(String(set.repsDone));
  const [weight, setWeight] = useState(
    set.weightKg != null ? String(set.weightKg) : ""
  );
  const [rpe, setRpe] = useState(set.rpe != null ? String(set.rpe) : "");

  const submit = () => {
    const payload: UpdateSetPayload = {};
    if (reps !== String(set.repsDone)) payload.repsDone = Number(reps);
    if (weight !== "") {
      if (set.weightKg == null || Number(weight) !== set.weightKg) {
        payload.weightKg = Number(weight);
      }
    }
    if (rpe !== "") {
      if (set.rpe == null || Number(rpe) !== set.rpe) {
        payload.rpe = Number(rpe);
      }
    }
    onSave(payload);
  };

  return (
    <div className="flex w-full flex-wrap items-end gap-2">
      <div>
        <Label htmlFor={`edit-reps-${set.id}`}>{t("reps")}</Label>
        <Input
          id={`edit-reps-${set.id}`}
          type="number"
          inputMode="numeric"
          min={0}
          value={reps}
          onChange={(e) => setReps(e.target.value)}
        />
      </div>
      <div>
        <Label htmlFor={`edit-weight-${set.id}`}>{t("weightKg")}</Label>
        <Input
          id={`edit-weight-${set.id}`}
          type="number"
          inputMode="decimal"
          step="0.5"
          min={0}
          value={weight}
          onChange={(e) => setWeight(e.target.value)}
        />
      </div>
      <div>
        <Label htmlFor={`edit-rpe-${set.id}`}>{t("rpe")}</Label>
        <Input
          id={`edit-rpe-${set.id}`}
          type="number"
          inputMode="numeric"
          min={1}
          max={10}
          value={rpe}
          onChange={(e) => setRpe(e.target.value)}
        />
      </div>
      <span className="flex gap-1">
        <Button size="sm" onClick={submit} disabled={pending}>
          {t("saveAction")}
        </Button>
        <Button
          size="sm"
          variant="ghost"
          onClick={onCancel}
          disabled={pending}
        >
          {t("cancelAction")}
        </Button>
      </span>
    </div>
  );
}

function formatSetLine(s: SessionSet): string {
  const head =
    s.weightKg != null
      ? `${s.setNumber}. ${s.repsDone} x ${s.weightKg}kg`
      : `${s.setNumber}. ${s.repsDone} reps`;
  if (s.rpe != null) return `${head} · RPE ${s.rpe}`;
  return head;
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
