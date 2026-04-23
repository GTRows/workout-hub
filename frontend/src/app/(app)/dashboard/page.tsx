"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import Link from "next/link";
import { useRouter } from "next/navigation";
import {
  fetchActiveSession,
  fetchActiveWorkoutPlan,
  startSession,
} from "@/lib/api/endpoints";
import type { SessionDetail, WorkoutDay } from "@/lib/api/schemas";
import { getTodayIsoDayOfWeek } from "@/lib/time/today";
import { Button } from "@/components/ui/button";
import { Card, CardDescription, CardTitle } from "@/components/ui/card";
import { PushPermissionCard } from "@/components/push-permission-card";

export default function DashboardPage() {
  const t = useTranslations("dashboard");
  const router = useRouter();
  const qc = useQueryClient();

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
  });

  if (planQuery.isLoading || sessionQuery.isLoading) {
    return <p className="text-muted-foreground">{t("loading")}</p>;
  }

  const active = sessionQuery.data ?? null;
  if (active) {
    return (
      <div className="space-y-4">
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
        <PushPermissionCard />
        <QuickActions />
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
        <PushPermissionCard />
        <QuickActions />
      </div>
    );
  }

  if (plan) {
    return (
      <div className="space-y-4">
        <Card>
          <CardTitle>{t("restDay")}</CardTitle>
        </Card>
        <PushPermissionCard />
        <QuickActions />
      </div>
    );
  }

  return (
    <Card className="space-y-3">
      <CardDescription>{t("noPlan")}</CardDescription>
      <Button asChild variant="outline">
        <Link href="/plan">{t("viewPlan")}</Link>
      </Button>
    </Card>
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
    </div>
  );
}
