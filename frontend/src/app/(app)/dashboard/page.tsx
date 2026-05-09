"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import {
  fetchActiveSession,
  fetchActiveWorkoutPlan,
  startSession,
} from "@/lib/api/endpoints";
import {
  ApiErrorCode,
  apiErrorCodeMessageKey,
  isApiErrorWithCode,
} from "@/lib/api/api-error-codes";
import type { SessionDetail, WorkoutDay } from "@/lib/api/schemas";
import { getTodayIsoDayOfWeek } from "@/lib/time/today";
import { Button } from "@/components/ui/button";
import { Card, CardDescription, CardTitle } from "@/components/ui/card";
import { InstallPromptCard } from "@/components/install-prompt-card";
import { PrToast } from "@/components/pr-toast";
import { PushPermissionCard } from "@/components/push-permission-card";
import { WeeklySummaryCard } from "@/components/dashboard/weekly-summary-card";
import { LastWeightCard } from "@/components/dashboard/last-weight-card";
import { RouteSkeleton } from "@/components/skeletons/route-skeleton";

export default function DashboardPage() {
  const t = useTranslations("dashboard");
  const tFull = useTranslations();
  const router = useRouter();
  const qc = useQueryClient();
  const [apiErrorToast, setApiErrorToast] = useState<string | null>(null);

  const planQuery = useQuery({
    queryKey: ["workout-plans", "active"],
    queryFn: fetchActiveWorkoutPlan,
  });
  const sessionQuery = useQuery({
    queryKey: ["sessions", "active"],
    queryFn: fetchActiveSession,
  });

  const startMutation = useMutation({
    mutationFn: (dayId: string | undefined) => startSession(dayId),
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

  if (planQuery.isLoading || sessionQuery.isLoading) {
    return <RouteSkeleton variant="dashboard" />;
  }

  const apiErrorToastNode = apiErrorToast ? (
    <PrToast
      message={apiErrorToast}
      onDismiss={() => setApiErrorToast(null)}
    />
  ) : null;

  const active = sessionQuery.data ?? null;
  if (active) {
    return (
      <div className="space-y-4">
        <InstallPromptCard />
        <Card className="space-y-4">
          <div className="space-y-1">
            <CardTitle>{t("resumeTitle")}</CardTitle>
            <CardDescription>{t("resumeHint")}</CardDescription>
          </div>
          <Button
            size="lg"
            className="w-full"
            onClick={() => router.push(`/session/${active.id}`)}
          >
            {t("resumeButton")}
          </Button>
        </Card>
        <WeeklySummaryCard />
        <LastWeightCard />
        <PushPermissionCard />
        <QuickActions />
        {apiErrorToastNode}
      </div>
    );
  }

  const plan = planQuery.data ?? null;
  const today = getTodayIsoDayOfWeek();
  const todayDay: WorkoutDay | undefined = plan?.days.find(
    (d) => d.dayOfWeek === today
  );

  if (plan && todayDay) {
    return (
      <div className="space-y-4">
        <InstallPromptCard />
        <Card className="space-y-4">
          <div className="space-y-1">
            <CardDescription>{t("todayLabel")}</CardDescription>
            <CardTitle>{todayDay.name}</CardTitle>
            <CardDescription>
              {t("exerciseCount", { count: todayDay.exercises.length })}
            </CardDescription>
          </div>
          <Button
            size="lg"
            className="w-full"
            onClick={() => startMutation.mutate(todayDay.id)}
            disabled={startMutation.isPending}
          >
            {t("startButton")}
          </Button>
        </Card>
        <WeeklySummaryCard />
        <LastWeightCard />
        <PushPermissionCard />
        <QuickActions />
        {apiErrorToastNode}
      </div>
    );
  }

  if (plan) {
    return (
      <div className="space-y-4">
        <InstallPromptCard />
        <Card>
          <CardTitle>{t("restDay")}</CardTitle>
        </Card>
        <WeeklySummaryCard />
        <LastWeightCard />
        <PushPermissionCard />
        <QuickActions />
        {apiErrorToastNode}
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <InstallPromptCard />
      <Card className="space-y-3">
        <CardDescription>{t("noPlan")}</CardDescription>
        <Button asChild variant="outline">
          <Link href="/plan">{t("viewPlan")}</Link>
        </Button>
      </Card>
      <WeeklySummaryCard />
      <LastWeightCard />
      <PushPermissionCard />
      <QuickActions />
      {apiErrorToastNode}
    </div>
  );
}

function QuickActions() {
  const t = useTranslations("dashboard");
  return (
    <div className="flex gap-2">
      <Button asChild variant="outline" size="sm">
        <Link href="/metrics">{t("quickAdd")}</Link>
      </Button>
      <Button asChild variant="ghost" size="sm">
        <Link href="/plan">{t("viewPlan")}</Link>
      </Button>
      <Button asChild variant="ghost" size="sm">
        <Link href="/history">{t("viewHistory")}</Link>
      </Button>
    </div>
  );
}
