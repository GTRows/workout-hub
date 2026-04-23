"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { useState } from "react";
import {
  createSupplement,
  deleteSupplement,
  fetchSupplements,
  type CreateSupplementPayload,
} from "@/lib/api/endpoints";
import {
  supplementTimingValues,
  type SupplementTiming,
} from "@/lib/api/schemas";
import { Button } from "@/components/ui/button";
import { Card, CardDescription, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

type FormState = {
  name: string;
  dosage: string;
  timing: SupplementTiming;
  reminderTime: string;
};

const EMPTY: FormState = {
  name: "",
  dosage: "",
  timing: "morning",
  reminderTime: "",
};

export function SupplementsSection() {
  const t = useTranslations("supplements");
  const qc = useQueryClient();

  const listQuery = useQuery({
    queryKey: ["supplements"],
    queryFn: fetchSupplements,
  });

  const [form, setForm] = useState<FormState>(EMPTY);

  const createMutation = useMutation({
    mutationFn: (payload: CreateSupplementPayload) => createSupplement(payload),
    onSuccess: () => {
      setForm(EMPTY);
      qc.invalidateQueries({ queryKey: ["supplements"] });
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => deleteSupplement(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["supplements"] }),
  });

  const submit = () => {
    if (!form.name.trim()) return;
    createMutation.mutate({
      name: form.name.trim(),
      dosage: form.dosage.trim() || undefined,
      timing: form.timing,
      reminderTime: form.reminderTime || undefined,
    });
  };

  const items = listQuery.data ?? [];

  return (
    <Card className="space-y-3">
      <CardTitle>{t("title")}</CardTitle>
      <CardDescription>{t("description")}</CardDescription>

      {items.length === 0 ? (
        <p className="text-muted-foreground">{t("empty")}</p>
      ) : (
        <ul className="space-y-1 text-sm" data-testid="supplements-list">
          {items.map((s) => (
            <li key={s.id} className="flex items-center justify-between gap-2">
              <span>
                <span className="font-medium">{s.name}</span>
                {s.dosage ? ` - ${s.dosage}` : ""}
                <span className="ml-2 text-xs text-muted-foreground">
                  {t(`timing.${s.timing}`)}
                  {s.reminderTime ? ` ${s.reminderTime.slice(0, 5)}` : ""}
                </span>
              </span>
              <Button
                size="sm"
                variant="ghost"
                onClick={() => {
                  if (window.confirm(t("deleteConfirm"))) {
                    deleteMutation.mutate(s.id);
                  }
                }}
              >
                {t("delete")}
              </Button>
            </li>
          ))}
        </ul>
      )}

      <div className="grid grid-cols-1 gap-2 sm:grid-cols-[1fr_1fr_auto_auto_auto]">
        <div>
          <Label htmlFor="supp-name">{t("name")}</Label>
          <Input
            id="supp-name"
            value={form.name}
            onChange={(e) => setForm((p) => ({ ...p, name: e.target.value }))}
          />
        </div>
        <div>
          <Label htmlFor="supp-dosage">{t("dosage")}</Label>
          <Input
            id="supp-dosage"
            value={form.dosage}
            onChange={(e) => setForm((p) => ({ ...p, dosage: e.target.value }))}
          />
        </div>
        <div>
          <Label htmlFor="supp-timing">{t("timing.label")}</Label>
          <select
            id="supp-timing"
            className="h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm"
            value={form.timing}
            onChange={(e) =>
              setForm((p) => ({
                ...p,
                timing: e.target.value as SupplementTiming,
              }))
            }
          >
            {supplementTimingValues.map((v) => (
              <option key={v} value={v}>
                {t(`timing.${v}`)}
              </option>
            ))}
          </select>
        </div>
        <div>
          <Label htmlFor="supp-reminder">{t("reminderTime")}</Label>
          <Input
            id="supp-reminder"
            type="time"
            value={form.reminderTime}
            onChange={(e) =>
              setForm((p) => ({ ...p, reminderTime: e.target.value }))
            }
          />
        </div>
        <div className="flex items-end">
          <Button onClick={submit} disabled={createMutation.isPending}>
            {createMutation.isPending ? t("saving") : t("add")}
          </Button>
        </div>
      </div>
    </Card>
  );
}
