"use client";

import { useQuery } from "@tanstack/react-query";
import { useLocale, useTranslations } from "next-intl";
import Link from "next/link";
import { useEffect, useState } from "react";
import { fetchExercises, searchExercises } from "@/lib/api/endpoints";
import type { Exercise } from "@/lib/api/schemas";
import { pickLocaleField } from "@/lib/locale";
import { Card, CardDescription, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { RouteSkeleton } from "@/components/skeletons/route-skeleton";

const CATEGORIES = ["push", "pull", "legs", "cardio", "core", "forearm"] as const;
const EQUIPMENTS = [
  "bodyweight",
  "dumbbell",
  "zbar",
  "bar",
  "wrist_tool",
  "machine",
  "other",
] as const;
const DIFFICULTIES = ["beginner", "intermediate", "advanced"] as const;

export function ExercisesClient() {
  const t = useTranslations("exercises");
  const locale = useLocale();
  const [category, setCategory] = useState<string>("");
  const [equipment, setEquipment] = useState<string>("");
  const [difficulty, setDifficulty] = useState<string>("");
  const [rawQ, setRawQ] = useState("");
  const [q, setQ] = useState("");

  useEffect(() => {
    const id = setTimeout(() => setQ(rawQ.trim()), 300);
    return () => clearTimeout(id);
  }, [rawQ]);

  const useSearch = q.length > 0;
  const listQuery = useQuery({
    queryKey: useSearch
      ? ["exercises", "search", q]
      : ["exercises", "filter", category, equipment, difficulty],
    queryFn: () =>
      useSearch
        ? searchExercises(q)
        : fetchExercises({
            category: category || undefined,
            equipment: equipment || undefined,
            difficulty: difficulty || undefined,
            size: 50,
          }),
  });

  const list: Exercise[] = listQuery.data?.content ?? [];

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-semibold tracking-tight">{t("title")}</h1>

      <div className="space-y-3">
        <Input
          type="search"
          placeholder={t("searchPlaceholder")}
          aria-label={t("searchPlaceholder")}
          value={rawQ}
          onChange={(e) => setRawQ(e.target.value)}
        />
        <div className="grid grid-cols-1 gap-2 sm:grid-cols-3">
          <FilterSelect
            label={t("filterCategoryLabel")}
            value={category}
            onChange={setCategory}
            options={CATEGORIES}
            formatOption={(k) => t(`category.${k}` as `category.push`)}
            anyLabel={t("filterAny")}
          />
          <FilterSelect
            label={t("filterEquipmentLabel")}
            value={equipment}
            onChange={setEquipment}
            options={EQUIPMENTS}
            formatOption={(k) => t(`equipment.${k}` as `equipment.bodyweight`)}
            anyLabel={t("filterAny")}
          />
          <FilterSelect
            label={t("filterDifficultyLabel")}
            value={difficulty}
            onChange={setDifficulty}
            options={DIFFICULTIES}
            formatOption={(k) => t(`difficulty.${k}` as `difficulty.beginner`)}
            anyLabel={t("filterAny")}
          />
        </div>
      </div>

      {listQuery.isLoading ? (
        <RouteSkeleton variant="exercises" />
      ) : list.length === 0 ? (
        <p className="text-muted-foreground">{t("empty")}</p>
      ) : (
        <ul className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {list.map((ex) => (
            <li key={ex.id}>
              <Link
                href={`/exercises/${ex.id}`}
                className="block focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary rounded-lg"
              >
                <Card className="space-y-1 transition hover:bg-muted/40">
                  <CardTitle className="text-base">
                    {pickLocaleField(locale, ex.nameTr, ex.nameEn)}
                  </CardTitle>
                  <CardDescription>
                    {t(`category.${ex.category}` as `category.push`)} ·{" "}
                    {t(`equipment.${ex.equipment}` as `equipment.bodyweight`)} ·{" "}
                    {t(`difficulty.${ex.difficulty}` as `difficulty.beginner`)}
                  </CardDescription>
                </Card>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

function FilterSelect({
  label,
  value,
  onChange,
  options,
  formatOption,
  anyLabel,
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  options: readonly string[];
  formatOption: (key: string) => string;
  anyLabel: string;
}) {
  const id = `filter-${label.toLowerCase().replace(/\s+/g, "-")}`;
  return (
    <div className="space-y-1">
      <Label htmlFor={id}>{label}</Label>
      <select
        id={id}
        className="h-10 w-full rounded-md border border-border bg-transparent px-3 text-sm"
        value={value}
        onChange={(e) => onChange(e.target.value)}
      >
        <option value="">{anyLabel}</option>
        {options.map((opt) => (
          <option key={opt} value={opt}>
            {formatOption(opt)}
          </option>
        ))}
      </select>
    </div>
  );
}
