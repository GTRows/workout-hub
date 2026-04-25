"use client";

import { useQuery } from "@tanstack/react-query";
import { useLocale, useTranslations } from "next-intl";
import Link from "next/link";
import { fetchExerciseDetail } from "@/lib/api/endpoints";
import { pickLocaleArray, pickLocaleField } from "@/lib/locale";
import { Card, CardDescription, CardTitle } from "@/components/ui/card";
import { ExerciseMedia } from "@/components/exercise-media";

export function ExerciseDetailClient({ id }: { id: string }) {
  const t = useTranslations("exercises");
  const locale = useLocale();

  const query = useQuery({
    queryKey: ["exercises", id],
    queryFn: () => fetchExerciseDetail(id),
  });

  if (query.isLoading) return <p className="text-muted-foreground">{t("loading")}</p>;
  if (query.isError || !query.data) {
    return <p className="text-destructive">{t("notFound")}</p>;
  }

  const ex = query.data;
  const name = pickLocaleField(locale, ex.nameTr, ex.nameEn);
  const description = pickLocaleField(locale, ex.descriptionTr, ex.descriptionEn);
  const tips = pickLocaleArray(locale, ex.formTipsTr, ex.formTipsEn);
  const mistakes = pickLocaleArray(locale, ex.commonMistakesTr, ex.commonMistakesEn);

  return (
    <div className="space-y-6">
      <Link
        href="/exercises"
        className="text-sm text-muted-foreground hover:text-foreground"
      >
        {t("backToCatalog")}
      </Link>

      <Card className="space-y-2">
        <CardTitle className="text-2xl">{name}</CardTitle>
        <CardDescription>
          {t(`category.${ex.category}` as `category.push`)} ·{" "}
          {t(`equipment.${ex.equipment}` as `equipment.bodyweight`)} ·{" "}
          {t(`difficulty.${ex.difficulty}` as `difficulty.beginner`)}
        </CardDescription>
        <CardDescription>
          <span className="font-medium">{t("musclePrimary")}:</span> {ex.musclePrimary}
          {ex.muscleSecondary ? (
            <>
              {" · "}
              <span className="font-medium">{t("muscleSecondary")}:</span>{" "}
              {ex.muscleSecondary}
            </>
          ) : null}
        </CardDescription>
      </Card>

      <ExerciseMedia
        videoUrl={ex.videoUrl}
        imageUrl={ex.imageUrl}
        alt={name}
      />

      <Card>
        <p className="whitespace-pre-line text-sm">
          {description || t("noDescription")}
        </p>
      </Card>

      {tips.length > 0 && (
        <Card className="space-y-2">
          <CardTitle className="text-base">{t("formTips")}</CardTitle>
          <ul className="list-inside list-disc space-y-1 text-sm">
            {tips.map((tip, i) => (
              <li key={i}>{tip}</li>
            ))}
          </ul>
        </Card>
      )}

      {mistakes.length > 0 && (
        <Card className="space-y-2">
          <CardTitle className="text-base">{t("commonMistakes")}</CardTitle>
          <ul className="list-inside list-disc space-y-1 text-sm">
            {mistakes.map((m, i) => (
              <li key={i}>{m}</li>
            ))}
          </ul>
        </Card>
      )}
    </div>
  );
}
