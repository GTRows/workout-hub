"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { useState } from "react";
import {
  activateWorkoutPlan,
  createWorkoutPlan,
  deleteWorkoutPlan,
  fetchWorkoutPlans,
  updateWorkoutPlan,
} from "@/lib/api/endpoints";
import type { WorkoutPlan } from "@/lib/api/schemas";
import { Button } from "@/components/ui/button";
import { Card, CardDescription, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { cn } from "@/lib/utils";
import { PlanRenameDialog } from "./plan-rename-dialog";

export function PlanList() {
  const t = useTranslations("plan");
  const qc = useQueryClient();

  const listQuery = useQuery({
    queryKey: ["workout-plans", "list"],
    queryFn: fetchWorkoutPlans,
  });

  const invalidateAll = () =>
    qc.invalidateQueries({ queryKey: ["workout-plans"] });

  const createMutation = useMutation({
    mutationFn: (name: string) => createWorkoutPlan({ name }),
    onSuccess: invalidateAll,
  });

  const updateMutation = useMutation({
    mutationFn: ({ planId, name }: { planId: string; name: string }) =>
      updateWorkoutPlan(planId, { name }),
    onSuccess: invalidateAll,
  });

  const deleteMutation = useMutation({
    mutationFn: (planId: string) => deleteWorkoutPlan(planId),
    onSuccess: invalidateAll,
  });

  const activateMutation = useMutation({
    mutationFn: (planId: string) => activateWorkoutPlan(planId),
    onSuccess: invalidateAll,
  });

  const [newPlanName, setNewPlanName] = useState("");
  const [createError, setCreateError] = useState<string | null>(null);
  const [renamingId, setRenamingId] = useState<string | null>(null);

  const handleCreateSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const trimmed = newPlanName.trim();
    if (!trimmed) {
      setCreateError(t("nameRequired"));
      return;
    }
    setCreateError(null);
    createMutation.mutate(trimmed, {
      onSuccess: () => setNewPlanName(""),
    });
  };

  return (
    <section className="space-y-3" aria-labelledby="plan-manage-heading">
      <h2
        id="plan-manage-heading"
        className="text-lg font-semibold tracking-tight"
      >
        {t("manageTitle")}
      </h2>

      <Card className="space-y-2">
        <form onSubmit={handleCreateSubmit} className="space-y-2">
          <Label htmlFor="new-plan-name">{t("newPlanNameLabel")}</Label>
          <div className="flex gap-2">
            <Input
              id="new-plan-name"
              value={newPlanName}
              onChange={(e) => setNewPlanName(e.target.value)}
              disabled={createMutation.isPending}
            />
            <Button
              type="submit"
              size="sm"
              disabled={createMutation.isPending}
            >
              {t("newPlanSubmit")}
            </Button>
          </div>
          {createError && (
            <p className="text-xs text-destructive">{createError}</p>
          )}
          {createMutation.isError && (
            <p className="text-xs text-destructive">{t("mutationError")}</p>
          )}
        </form>
      </Card>

      {listQuery.isLoading && (
        <p className="text-muted-foreground">{t("loading")}</p>
      )}
      {listQuery.isError && (
        <p className="text-destructive">{t("mutationError")}</p>
      )}
      {listQuery.data && listQuery.data.length === 0 && (
        <Card>
          <CardDescription>{t("noPlans")}</CardDescription>
        </Card>
      )}
      {listQuery.data && listQuery.data.length > 0 && (
        <ul className="space-y-2" data-testid="plan-list">
          {listQuery.data.map((plan) => (
            <li key={plan.id}>
              <Card
                className={cn("space-y-2", plan.active && "border-primary")}
                data-testid={`plan-row-${plan.id}`}
              >
                {renamingId === plan.id ? (
                  <PlanRenameDialog
                    initialName={plan.name}
                    pending={updateMutation.isPending}
                    onSubmit={(newName) => {
                      updateMutation.mutate(
                        { planId: plan.id, name: newName },
                        { onSuccess: () => setRenamingId(null) }
                      );
                    }}
                    onCancel={() => setRenamingId(null)}
                  />
                ) : (
                  <PlanRow
                    plan={plan}
                    onActivate={() => activateMutation.mutate(plan.id)}
                    onRename={() => setRenamingId(plan.id)}
                    onDelete={() => {
                      if (window.confirm(t("deleteConfirm"))) {
                        deleteMutation.mutate(plan.id);
                      }
                    }}
                    activatePending={activateMutation.isPending}
                    deletePending={deleteMutation.isPending}
                    t={t}
                  />
                )}
              </Card>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}

function PlanRow({
  plan,
  onActivate,
  onRename,
  onDelete,
  activatePending,
  deletePending,
  t,
}: {
  plan: WorkoutPlan;
  onActivate: () => void;
  onRename: () => void;
  onDelete: () => void;
  activatePending: boolean;
  deletePending: boolean;
  t: ReturnType<typeof useTranslations<"plan">>;
}) {
  return (
    <div className="flex items-center justify-between gap-2">
      <div className="flex-1">
        <CardTitle>{plan.name}</CardTitle>
        {plan.active && (
          <span className="mt-1 inline-block rounded bg-primary/10 px-2 py-0.5 text-xs text-primary">
            {t("activeBadge")}
          </span>
        )}
      </div>
      <div className="flex flex-wrap gap-2">
        {!plan.active && (
          <Button
            variant="outline"
            size="sm"
            onClick={onActivate}
            disabled={activatePending}
            data-testid={`activate-${plan.id}`}
          >
            {t("activate")}
          </Button>
        )}
        <Button
          variant="ghost"
          size="sm"
          onClick={onRename}
          data-testid={`rename-${plan.id}`}
        >
          {t("rename")}
        </Button>
        <Button
          variant="ghost"
          size="sm"
          onClick={onDelete}
          disabled={plan.active || deletePending}
          aria-label={plan.active ? t("cannotDeleteActive") : t("delete")}
          data-testid={`delete-${plan.id}`}
        >
          {t("delete")}
        </Button>
      </div>
    </div>
  );
}
