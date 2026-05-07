"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { useEffect, useState } from "react";
import { fetchMe, updateMe, type UpdateProfilePayload } from "@/lib/api/endpoints";
import type { UserMe } from "@/lib/api/schemas";
import { LogoutButton } from "@/components/logout-button";
import { Button } from "@/components/ui/button";
import { Card, CardDescription, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { SupplementsSection } from "./supplements-section";
import { WebhookTokensSection } from "./webhook-tokens-section";

type FormState = {
  displayName: string;
  heightCm: string;
  weightKg: string;
  birthDate: string;
  gender: string;
  healthNotes: string;
  goals: string;
  dailyKcalGoal: string;
  dailyProteinGGoal: string;
  dailyCarbsGGoal: string;
  dailyFatGGoal: string;
};

const EMPTY_FORM: FormState = {
  displayName: "",
  heightCm: "",
  weightKg: "",
  birthDate: "",
  gender: "",
  healthNotes: "",
  goals: "",
  dailyKcalGoal: "",
  dailyProteinGGoal: "",
  dailyCarbsGGoal: "",
  dailyFatGGoal: "",
};

export function ProfileClient() {
  const t = useTranslations("profile");
  const qc = useQueryClient();

  const meQuery = useQuery({ queryKey: ["users", "me"], queryFn: fetchMe });
  const [form, setForm] = useState<FormState>(EMPTY_FORM);
  const [flash, setFlash] = useState<"saved" | "error" | null>(null);

  useEffect(() => {
    if (!meQuery.data) return;
    setForm(toForm(meQuery.data));
  }, [meQuery.data]);

  const mutation = useMutation({
    mutationFn: (payload: UpdateProfilePayload) => updateMe(payload),
    onSuccess: (fresh) => {
      qc.setQueryData(["users", "me"], fresh);
      setFlash("saved");
    },
    onError: () => setFlash("error"),
  });

  if (meQuery.isLoading || !meQuery.data) {
    return <p className="text-muted-foreground">{t("loading")}</p>;
  }

  const submit = () => {
    setFlash(null);
    const payload: UpdateProfilePayload = {};
    if (form.displayName) payload.displayName = form.displayName;
    if (form.heightCm) payload.heightCm = Number(form.heightCm);
    if (form.weightKg) payload.weightKg = Number(form.weightKg);
    if (form.birthDate) payload.birthDate = form.birthDate;
    if (form.gender) payload.gender = form.gender;
    if (form.healthNotes) payload.healthNotes = form.healthNotes;
    if (form.goals) payload.goals = form.goals;
    if (form.dailyKcalGoal) payload.dailyKcalGoal = Number(form.dailyKcalGoal);
    if (form.dailyProteinGGoal) payload.dailyProteinGGoal = Number(form.dailyProteinGGoal);
    if (form.dailyCarbsGGoal) payload.dailyCarbsGGoal = Number(form.dailyCarbsGGoal);
    if (form.dailyFatGGoal) payload.dailyFatGGoal = Number(form.dailyFatGGoal);
    mutation.mutate(payload);
  };

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-semibold tracking-tight">{t("title")}</h1>

      <Card className="space-y-3">
        <CardTitle>{meQuery.data.email}</CardTitle>
        <CardDescription>{t("email")}</CardDescription>
      </Card>

      <Card className="space-y-3">
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
          <Field id="displayName" label={t("displayName")}>
            <Input
              id="displayName"
              value={form.displayName}
              onChange={(e) =>
                setForm((p) => ({ ...p, displayName: e.target.value }))
              }
            />
          </Field>
          <Field id="heightCm" label={t("heightCm")}>
            <Input
              id="heightCm"
              type="number"
              inputMode="numeric"
              value={form.heightCm}
              onChange={(e) =>
                setForm((p) => ({ ...p, heightCm: e.target.value }))
              }
            />
          </Field>
          <Field id="weightKg" label={t("weightKg")}>
            <Input
              id="weightKg"
              type="number"
              inputMode="decimal"
              step="0.1"
              value={form.weightKg}
              onChange={(e) =>
                setForm((p) => ({ ...p, weightKg: e.target.value }))
              }
            />
          </Field>
          <Field id="birthDate" label={t("birthDate")}>
            <Input
              id="birthDate"
              type="date"
              value={form.birthDate}
              onChange={(e) =>
                setForm((p) => ({ ...p, birthDate: e.target.value }))
              }
            />
          </Field>
          <Field id="gender" label={t("gender")}>
            <Input
              id="gender"
              value={form.gender}
              onChange={(e) =>
                setForm((p) => ({ ...p, gender: e.target.value }))
              }
            />
          </Field>
        </div>

        <Field id="healthNotes" label={t("healthNotes")}>
          <Input
            id="healthNotes"
            value={form.healthNotes}
            onChange={(e) =>
              setForm((p) => ({ ...p, healthNotes: e.target.value }))
            }
          />
        </Field>

        <Field id="goals" label={t("goals")}>
          <Input
            id="goals"
            value={form.goals}
            onChange={(e) => setForm((p) => ({ ...p, goals: e.target.value }))}
          />
        </Field>

        <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
          <Field id="dailyKcalGoal" label={t("dailyKcalGoal")}>
            <Input
              id="dailyKcalGoal"
              type="number"
              inputMode="numeric"
              value={form.dailyKcalGoal}
              onChange={(e) => setForm((p) => ({ ...p, dailyKcalGoal: e.target.value }))}
            />
          </Field>
          <Field id="dailyProteinGGoal" label={t("dailyProteinGGoal")}>
            <Input
              id="dailyProteinGGoal"
              type="number"
              inputMode="numeric"
              value={form.dailyProteinGGoal}
              onChange={(e) => setForm((p) => ({ ...p, dailyProteinGGoal: e.target.value }))}
            />
          </Field>
          <Field id="dailyCarbsGGoal" label={t("dailyCarbsGGoal")}>
            <Input
              id="dailyCarbsGGoal"
              type="number"
              inputMode="numeric"
              value={form.dailyCarbsGGoal}
              onChange={(e) => setForm((p) => ({ ...p, dailyCarbsGGoal: e.target.value }))}
            />
          </Field>
          <Field id="dailyFatGGoal" label={t("dailyFatGGoal")}>
            <Input
              id="dailyFatGGoal"
              type="number"
              inputMode="numeric"
              value={form.dailyFatGGoal}
              onChange={(e) => setForm((p) => ({ ...p, dailyFatGGoal: e.target.value }))}
            />
          </Field>
        </div>

        <div className="flex items-center gap-3">
          <Button onClick={submit} disabled={mutation.isPending}>
            {mutation.isPending ? t("saving") : t("save")}
          </Button>
          {flash === "saved" && (
            <span className="text-sm text-primary">{t("saved")}</span>
          )}
          {flash === "error" && (
            <span role="alert" className="text-sm text-destructive">
              {t("error")}
            </span>
          )}
        </div>
      </Card>

      <SupplementsSection />
      <WebhookTokensSection />

      <section className="mt-8 border-t border-border pt-6">
        <h2 className="mb-3 text-sm font-semibold text-muted-foreground">
          {t("logoutHeading")}
        </h2>
        <LogoutButton variant="profile" />
      </section>
    </div>
  );
}

function Field({
  id,
  label,
  children,
}: {
  id: string;
  label: string;
  children: React.ReactNode;
}) {
  return (
    <div className="space-y-1">
      <Label htmlFor={id}>{label}</Label>
      {children}
    </div>
  );
}

function toForm(me: UserMe): FormState {
  return {
    displayName: me.displayName ?? "",
    heightCm: me.profile.heightCm != null ? String(me.profile.heightCm) : "",
    weightKg: me.profile.weightKg != null ? String(me.profile.weightKg) : "",
    birthDate: me.profile.birthDate ?? "",
    gender: me.profile.gender ?? "",
    healthNotes: me.profile.healthNotes ?? "",
    goals: me.profile.goals ?? "",
    dailyKcalGoal:
      me.profile.dailyKcalGoal != null ? String(me.profile.dailyKcalGoal) : "",
    dailyProteinGGoal:
      me.profile.dailyProteinGGoal != null ? String(me.profile.dailyProteinGGoal) : "",
    dailyCarbsGGoal:
      me.profile.dailyCarbsGGoal != null ? String(me.profile.dailyCarbsGGoal) : "",
    dailyFatGGoal:
      me.profile.dailyFatGGoal != null ? String(me.profile.dailyFatGGoal) : "",
  };
}
