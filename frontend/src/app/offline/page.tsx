import { getTranslations } from "next-intl/server";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { Card, CardDescription, CardTitle } from "@/components/ui/card";

export default async function OfflinePage() {
  const t = await getTranslations("pwa");
  return (
    <main className="mx-auto flex min-h-screen max-w-md flex-col items-center justify-center px-4">
      <Card className="w-full space-y-4">
        <div className="space-y-1">
          <CardTitle>{t("offlineTitle")}</CardTitle>
          <CardDescription>{t("offlineDescription")}</CardDescription>
        </div>
        <Button asChild variant="outline" className="w-full">
          <Link href="/dashboard">{t("offlineRetryLink")}</Link>
        </Button>
      </Card>
    </main>
  );
}
