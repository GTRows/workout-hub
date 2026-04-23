"use client";

import Link from "next/link";
import { useTranslations } from "next-intl";
import { usePathname } from "next/navigation";
import { cn } from "@/lib/utils";

const LINKS: Array<{ href: string; labelKey: "dashboard" | "plan" | "history" | "exercises" | "insights" | "prs" | "metrics" | "profile" | "export" }> = [
  { href: "/dashboard", labelKey: "dashboard" },
  { href: "/plan", labelKey: "plan" },
  { href: "/history", labelKey: "history" },
  { href: "/exercises", labelKey: "exercises" },
  { href: "/insights", labelKey: "insights" },
  { href: "/prs", labelKey: "prs" },
  { href: "/metrics", labelKey: "metrics" },
  { href: "/profile", labelKey: "profile" },
  { href: "/export", labelKey: "export" },
];

export function Nav() {
  const t = useTranslations("nav");
  const pathname = usePathname();

  return (
    <nav className="border-b border-border bg-background/80 backdrop-blur">
      <div className="mx-auto flex max-w-6xl items-center gap-1 overflow-x-auto px-4 py-2">
        {LINKS.map(({ href, labelKey }) => {
          const active = pathname.startsWith(href);
          return (
            <Link
              key={href}
              href={href}
              className={cn(
                "rounded-md px-3 py-1.5 text-sm font-medium whitespace-nowrap transition-colors",
                active
                  ? "bg-primary text-primary-foreground"
                  : "text-muted-foreground hover:text-foreground"
              )}
            >
              {t(labelKey)}
            </Link>
          );
        })}
      </div>
    </nav>
  );
}
