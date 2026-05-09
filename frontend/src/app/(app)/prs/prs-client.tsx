"use client";

import { useQuery } from "@tanstack/react-query";
import { useLocale, useTranslations } from "next-intl";
import { fetchPersonalRecords } from "@/lib/api/endpoints";
import { Card, CardDescription, CardTitle } from "@/components/ui/card";
import { RouteSkeleton } from "@/components/skeletons/route-skeleton";
import { pickLocaleField } from "@/lib/locale";

export function PrsClient() {
  const t = useTranslations("prs");
  const locale = useLocale();

  const query = useQuery({
    queryKey: ["analytics", "prs"],
    queryFn: fetchPersonalRecords,
  });

  if (query.isLoading) {
    return <RouteSkeleton variant="prs" />;
  }

  const prs = query.data ?? [];

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-semibold tracking-tight">{t("title")}</h1>

      {prs.length === 0 ? (
        <p className="text-muted-foreground">{t("empty")}</p>
      ) : (
        <ul className="space-y-2">
          {prs.map((pr) => (
            <li key={pr.exerciseId}>
              <Card className="space-y-1">
                <CardTitle className="text-base">
                  {pickLocaleField(locale, pr.exerciseNameTr, pr.exerciseNameEn)}
                </CardTitle>
                <CardDescription className="flex items-center justify-between gap-2">
                  <span>
                    {pr.weightKg ?? 0} kg x {pr.repsDone}{" "}
                    <span className="text-muted-foreground">({pr.achievedAt})</span>
                  </span>
                  <span className="rounded bg-primary/10 px-2 py-0.5 text-xs font-medium text-primary">
                    {t("oneRm", { value: pr.estimatedOneRmKg })}
                  </span>
                </CardDescription>
              </Card>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
