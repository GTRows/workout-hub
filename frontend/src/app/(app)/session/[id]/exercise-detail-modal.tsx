"use client";

import { useQuery } from "@tanstack/react-query";
import { useLocale, useTranslations } from "next-intl";
import { useEffect, useRef } from "react";
import { Button } from "@/components/ui/button";
import { fetchExerciseDetail, fetchLastPerformance } from "@/lib/api/endpoints";
import type { Exercise, LastPerformance, SessionSet } from "@/lib/api/schemas";

type Props = {
  exerciseId: string;
  onClose: () => void;
};

function formatSet(s: SessionSet): string {
  if (s.weightKg == null) return `${s.setNumber}. ${s.repsDone}`;
  return `${s.setNumber}. ${s.repsDone} x ${s.weightKg}`;
}

export function ExerciseDetailModal({ exerciseId, onClose }: Props) {
  const t = useTranslations("exercises");
  const locale = useLocale();
  const dialogRef = useRef<HTMLDivElement>(null);

  const exQuery = useQuery({
    queryKey: ["exercise-detail", exerciseId],
    queryFn: () => fetchExerciseDetail(exerciseId),
  });
  const perfQuery = useQuery({
    queryKey: ["last-performance", exerciseId],
    queryFn: () => fetchLastPerformance(exerciseId),
  });

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
    };
    document.addEventListener("keydown", onKey);
    return () => document.removeEventListener("keydown", onKey);
  }, [onClose]);

  useEffect(() => {
    dialogRef.current?.focus();
  }, []);

  const ex: Exercise | undefined = exQuery.data;
  const perf: LastPerformance | null | undefined = perfQuery.data ?? null;

  const tips = ex
    ? locale === "tr"
      ? ex.formTipsTr
      : ex.formTipsEn
    : [];
  const mistakes = ex
    ? locale === "tr"
      ? ex.commonMistakesTr
      : ex.commonMistakesEn
    : [];
  const name = ex ? (locale === "tr" ? ex.nameTr : ex.nameEn) : "";

  return (
    <div
      role="presentation"
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
      className="fixed inset-0 z-50 flex items-center justify-center bg-background/80 p-4"
      data-testid="exercise-detail-backdrop"
    >
      <div
        ref={dialogRef}
        role="dialog"
        aria-modal="true"
        aria-labelledby="exercise-detail-title"
        tabIndex={-1}
        className="max-h-[80vh] w-full max-w-md space-y-4 overflow-y-auto rounded-lg border border-border bg-background p-5 shadow-lg"
        data-testid="exercise-detail-modal"
      >
        <div className="flex items-start justify-between gap-2">
          <h2 id="exercise-detail-title" className="text-lg font-semibold">
            {name || t("loading")}
          </h2>
          <Button
            variant="outline"
            size="sm"
            onClick={onClose}
            data-testid="exercise-detail-close"
            aria-label="Close"
          >
            X
          </Button>
        </div>

        {tips.length > 0 && (
          <section data-testid="exercise-detail-tips" className="space-y-1">
            <h3 className="text-sm font-medium">{t("formTips")}</h3>
            <ul className="list-disc pl-5 text-sm text-muted-foreground">
              {tips.map((tip, i) => (
                <li key={i}>{tip}</li>
              ))}
            </ul>
          </section>
        )}

        {mistakes.length > 0 && (
          <section data-testid="exercise-detail-mistakes" className="space-y-1">
            <h3 className="text-sm font-medium">{t("commonMistakes")}</h3>
            <ul className="list-disc pl-5 text-sm text-muted-foreground">
              {mistakes.map((m, i) => (
                <li key={i}>{m}</li>
              ))}
            </ul>
          </section>
        )}

        {perf && perf.sets.length > 0 && (
          <section data-testid="exercise-detail-last-perf" className="space-y-1">
            <h3 className="text-sm font-medium">Last performance</h3>
            <ul className="list-disc pl-5 text-sm text-muted-foreground">
              {perf.sets.slice(0, 5).map((s) => (
                <li key={s.id}>{formatSet(s)}</li>
              ))}
            </ul>
          </section>
        )}
      </div>
    </div>
  );
}
