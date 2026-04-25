"use client";

import { useQuery } from "@tanstack/react-query";
import { useLocale, useTranslations } from "next-intl";
import { Card, CardDescription, CardTitle } from "@/components/ui/card";
import { fetchAchievements } from "@/lib/api/endpoints";
import type { Achievement } from "@/lib/api/schemas";

const ICON_FALLBACK = "trophy";

function pickName(a: Achievement, locale: string) {
  return locale === "tr" ? a.nameTr : a.nameEn;
}
function pickDescription(a: Achievement, locale: string) {
  return locale === "tr" ? a.descriptionTr : a.descriptionEn;
}

export function AchievementsClient() {
  const t = useTranslations("achievements");
  const locale = useLocale();

  const query = useQuery({
    queryKey: ["achievements", "me"],
    queryFn: fetchAchievements,
  });

  if (query.isLoading) {
    return (
      <p className="text-sm text-muted-foreground">{t("loading")}</p>
    );
  }
  if (!query.data) {
    return (
      <p className="text-sm text-destructive" role="alert">
        {t("error")}
      </p>
    );
  }

  const unlocked = query.data.filter((a) => a.unlocked);
  const locked = query.data.filter((a) => !a.unlocked);

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-semibold tracking-tight">{t("title")}</h1>
      <p className="text-sm text-muted-foreground" data-testid="ach-summary">
        {t("summary", { unlocked: unlocked.length, total: query.data.length })}
      </p>

      <section className="space-y-2" data-testid="ach-unlocked-section">
        <h2 className="text-lg font-medium">{t("unlockedHeading")}</h2>
        {unlocked.length === 0 ? (
          <p className="text-sm text-muted-foreground">{t("unlockedEmpty")}</p>
        ) : (
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            {unlocked.map((a) => (
              <Card key={a.id} className="space-y-1" data-testid={`ach-card-${a.code}`}>
                <CardTitle className="text-base">
                  <span aria-hidden="true" className="mr-2">
                    {iconChar(a.icon ?? ICON_FALLBACK)}
                  </span>
                  {pickName(a, locale)}
                </CardTitle>
                <CardDescription>{pickDescription(a, locale)}</CardDescription>
                {a.unlockedAt && (
                  <p className="text-xs text-muted-foreground">
                    {t("unlockedAt", { time: new Date(a.unlockedAt).toLocaleDateString() })}
                  </p>
                )}
              </Card>
            ))}
          </div>
        )}
      </section>

      <section className="space-y-2" data-testid="ach-locked-section">
        <h2 className="text-lg font-medium">{t("nextHeading")}</h2>
        {locked.length === 0 ? (
          <p className="text-sm text-muted-foreground">{t("nextEmpty")}</p>
        ) : (
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            {locked.slice(0, 6).map((a) => (
              <Card
                key={a.id}
                className="space-y-1 opacity-60"
                data-testid={`ach-card-${a.code}`}
              >
                <CardTitle className="text-base">
                  <span aria-hidden="true" className="mr-2">
                    {iconChar(a.icon ?? ICON_FALLBACK)}
                  </span>
                  {pickName(a, locale)}
                </CardTitle>
                <CardDescription>{pickDescription(a, locale)}</CardDescription>
                <p className="text-xs text-muted-foreground">
                  {t("threshold", {
                    value: a.threshold,
                    rule: t(`rule.${a.ruleType}` as MessageKey),
                  })}
                </p>
              </Card>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}

type MessageKey = string;

function iconChar(icon: string): string {
  switch (icon) {
    case "flame":
      return "*";
    case "medal":
      return "*";
    case "muscle":
      return "*";
    default:
      return "*";
  }
}
