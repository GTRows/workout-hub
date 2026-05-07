"use client";

import { useQueryClient } from "@tanstack/react-query";
import { LogOut } from "lucide-react";
import { useTranslations } from "next-intl";
import { useRouter } from "next/navigation";
import { Button } from "@/components/ui/button";
import { clearTokens } from "@/lib/auth/token-store";
import { cn } from "@/lib/utils";

type Props = {
  variant?: "nav" | "profile";
};

export function LogoutButton({ variant = "nav" }: Props) {
  const t = useTranslations("nav");
  const router = useRouter();
  const queryClient = useQueryClient();
  const label = t("logout");

  // The order matters: redirect first so useSessionExpiredRedirect's subscriber
  // observes the access-token clear from the /login pathname and short-circuits
  // (no ?reason=session-expired flag for a manual logout).
  const onClick = async () => {
    router.replace("/login");
    await Promise.resolve();
    queryClient.clear();
    clearTokens();
  };

  if (variant === "profile") {
    return (
      <Button
        type="button"
        variant="destructive"
        onClick={onClick}
        aria-label={label}
        data-testid="logout-button"
      >
        <LogOut className="h-4 w-4" />
        {label}
      </Button>
    );
  }

  return (
    <button
      type="button"
      onClick={onClick}
      aria-label={label}
      data-testid="logout-button"
      className={cn(
        "inline-flex items-center gap-1 rounded-md px-3 py-1.5 text-sm font-medium whitespace-nowrap text-muted-foreground transition-colors hover:text-foreground"
      )}
    >
      <LogOut className="h-4 w-4" />
      {label}
    </button>
  );
}
