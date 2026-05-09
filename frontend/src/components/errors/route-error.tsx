// Shared error boundary fallback for (app) segments.
"use client";

import Link from "next/link";
import { useTranslations } from "next-intl";
import { useEffect, type ReactElement } from "react";
import { Button } from "@/components/ui/button";
import { Card, CardDescription, CardTitle } from "@/components/ui/card";

export interface RouteErrorProps {
  error: Error & { digest?: string };
  reset: () => void;
}

export function RouteError({ error, reset }: RouteErrorProps): ReactElement {
  const t = useTranslations("common");

  useEffect(() => {
    console.error("Route error", {
      message: error.message,
      digest: error.digest,
    });
  }, [error]);

  return (
    <Card role="alert" className="space-y-4">
      <div className="space-y-1">
        <CardTitle>{t("error")}</CardTitle>
        <CardDescription>{t("errorHint")}</CardDescription>
      </div>
      <div className="flex flex-wrap gap-2">
        <Button onClick={() => reset()}>{t("tryAgain")}</Button>
        <Button asChild variant="outline">
          <Link href="/dashboard">{t("goHome")}</Link>
        </Button>
      </div>
    </Card>
  );
}
