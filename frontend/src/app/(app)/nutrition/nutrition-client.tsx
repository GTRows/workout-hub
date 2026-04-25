"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useLocale, useTranslations } from "next-intl";
import { useMemo, useState } from "react";
import {
  createNutritionEntry,
  deleteNutritionEntry,
  fetchNutritionForDate,
  searchFoods,
  type CreateNutritionEntryPayload,
} from "@/lib/api/endpoints";
import { Button } from "@/components/ui/button";
import { Card, CardDescription, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { pickLocaleField } from "@/lib/locale";

function todayIso(): string {
  return new Date().toISOString().slice(0, 10);
}

function shiftDay(date: string, deltaDays: number): string {
  const d = new Date(date + "T00:00:00");
  d.setDate(d.getDate() + deltaDays);
  return d.toISOString().slice(0, 10);
}

export function NutritionClient() {
  const t = useTranslations("nutrition");
  const locale = useLocale();
  const qc = useQueryClient();

  const [date, setDate] = useState<string>(todayIso());
  const [query, setQuery] = useState("");
  const [pickedFoodId, setPickedFoodId] = useState<string>("");
  const [serving, setServing] = useState<string>("");

  const dayQuery = useQuery({
    queryKey: ["nutrition", date],
    queryFn: () => fetchNutritionForDate(date),
  });

  const searchQuery = useQuery({
    queryKey: ["foods", query],
    queryFn: () => searchFoods(query, 20),
    enabled: query.trim().length > 0,
  });

  const createMutation = useMutation({
    mutationFn: (payload: CreateNutritionEntryPayload) =>
      createNutritionEntry(payload),
    onSuccess: () => {
      setQuery("");
      setPickedFoodId("");
      setServing("");
      qc.invalidateQueries({ queryKey: ["nutrition", date] });
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => deleteNutritionEntry(id),
    onSuccess: () =>
      qc.invalidateQueries({ queryKey: ["nutrition", date] }),
  });

  const entries = dayQuery.data ?? [];
  const totals = useMemo(() => {
    return entries.reduce(
      (acc, e) => ({
        kcal: acc.kcal + (e.kcal ?? 0),
        protein: acc.protein + (e.proteinG ?? 0),
        carbs: acc.carbs + (e.carbsG ?? 0),
        fat: acc.fat + (e.fatG ?? 0),
      }),
      { kcal: 0, protein: 0, carbs: 0, fat: 0 }
    );
  }, [entries]);

  const submit = () => {
    if (!pickedFoodId) return;
    const grams = Number(serving);
    if (!Number.isFinite(grams) || grams <= 0) return;
    createMutation.mutate({
      foodId: pickedFoodId,
      servingG: grams,
      consumedAt: new Date().toISOString(),
    });
  };

  const pickedFoodName = (() => {
    const found = (searchQuery.data ?? []).find((f) => f.id === pickedFoodId);
    if (!found) return "";
    return pickLocaleField(locale, found.nameTr, found.nameEn);
  })();

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-semibold tracking-tight">{t("title")}</h1>

      <Card className="space-y-3" data-testid="nutrition-day-nav">
        <div className="flex items-center justify-between gap-2">
          <Button
            variant="outline"
            size="sm"
            onClick={() => setDate((d) => shiftDay(d, -1))}
            data-testid="day-prev"
          >
            {t("prevDay")}
          </Button>
          <span className="text-sm font-medium">{date}</span>
          <Button
            variant="outline"
            size="sm"
            onClick={() => setDate((d) => shiftDay(d, 1))}
            data-testid="day-next"
          >
            {t("nextDay")}
          </Button>
        </div>
        <CardDescription>
          {t("dayTotals", {
            kcal: totals.kcal.toFixed(0),
            protein: totals.protein.toFixed(0),
            carbs: totals.carbs.toFixed(0),
            fat: totals.fat.toFixed(0),
          })}
        </CardDescription>
      </Card>

      <Card className="space-y-3" data-testid="nutrition-add-form">
        <CardTitle className="text-base">{t("addTitle")}</CardTitle>
        <div className="grid grid-cols-1 gap-2 sm:grid-cols-[2fr_1fr_auto]">
          <div>
            <Label htmlFor="food-search">{t("searchLabel")}</Label>
            <Input
              id="food-search"
              value={query}
              onChange={(e) => {
                setQuery(e.target.value);
                setPickedFoodId("");
              }}
              placeholder={t("searchPlaceholder")}
            />
            {searchQuery.data && searchQuery.data.length > 0 && !pickedFoodId && (
              <ul
                className="mt-1 max-h-40 overflow-auto rounded-md border border-border text-sm"
                data-testid="food-suggestions"
              >
                {searchQuery.data.map((f) => (
                  <li key={f.id}>
                    <button
                      type="button"
                      className="block w-full px-3 py-1.5 text-left hover:bg-muted"
                      onClick={() => {
                        setPickedFoodId(f.id);
                        setQuery(pickLocaleField(locale, f.nameTr, f.nameEn));
                        if (!serving) setServing(String(f.defaultServingG));
                      }}
                    >
                      {pickLocaleField(locale, f.nameTr, f.nameEn)}
                      <span className="ml-2 text-xs text-muted-foreground">
                        {f.kcalPer100g} kcal/100g
                      </span>
                    </button>
                  </li>
                ))}
              </ul>
            )}
            {pickedFoodId && (
              <p className="mt-1 text-xs text-muted-foreground">
                {t("selectedHint", { name: pickedFoodName })}
              </p>
            )}
          </div>
          <div>
            <Label htmlFor="food-serving">{t("servingLabel")}</Label>
            <Input
              id="food-serving"
              type="number"
              inputMode="numeric"
              value={serving}
              onChange={(e) => setServing(e.target.value)}
            />
          </div>
          <div className="flex items-end">
            <Button
              onClick={submit}
              disabled={!pickedFoodId || createMutation.isPending}
            >
              {createMutation.isPending ? t("saving") : t("add")}
            </Button>
          </div>
        </div>
      </Card>

      <Card className="space-y-2" data-testid="nutrition-list">
        <CardTitle className="text-base">{t("dayList")}</CardTitle>
        {entries.length === 0 ? (
          <p className="text-muted-foreground">{t("empty")}</p>
        ) : (
          <ul className="space-y-1 text-sm">
            {entries.map((e) => (
              <li
                key={e.id}
                className="flex items-center justify-between gap-2"
              >
                <span>
                  <span className="font-medium">
                    {pickLocaleField(
                      locale,
                      e.foodNameTr ?? "",
                      e.foodNameEn ?? ""
                    )}
                  </span>{" "}
                  - {e.servingG}g{" "}
                  <span className="text-xs text-muted-foreground">
                    ({Math.round(e.kcal ?? 0)} kcal,{" "}
                    {Math.round(e.proteinG ?? 0)}p,{" "}
                    {Math.round(e.carbsG ?? 0)}c,{" "}
                    {Math.round(e.fatG ?? 0)}f)
                  </span>
                </span>
                <Button
                  size="sm"
                  variant="ghost"
                  onClick={() => {
                    if (window.confirm(t("deleteConfirm"))) {
                      deleteMutation.mutate(e.id);
                    }
                  }}
                  data-testid={`delete-${e.id}`}
                >
                  {t("delete")}
                </Button>
              </li>
            ))}
          </ul>
        )}
      </Card>
    </div>
  );
}
