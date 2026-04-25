"use client";

import Link from "next/link";
import { useTranslations } from "next-intl";
import { usePathname } from "next/navigation";
import { cn } from "@/lib/utils";

type LabelKey =
  | "dashboard"
  | "plan"
  | "history"
  | "exercises"
  | "nutrition";

const LINKS: Array<{ href: string; labelKey: LabelKey }> = [
  { href: "/dashboard", labelKey: "dashboard" },
  { href: "/plan", labelKey: "plan" },
  { href: "/history", labelKey: "history" },
  { href: "/exercises", labelKey: "exercises" },
  { href: "/nutrition", labelKey: "nutrition" },
];

export function BottomNav() {
  const t = useTranslations("nav");
  const pathname = usePathname();

  return (
    <nav
      aria-label="Bottom navigation"
      data-testid="bottom-nav"
      className="fixed inset-x-0 bottom-0 z-40 border-t border-border bg-background/95 backdrop-blur sm:hidden"
    >
      <ul className="mx-auto flex max-w-md items-center justify-around px-2 py-2">
        {LINKS.map(({ href, labelKey }) => {
          const active = pathname.startsWith(href);
          return (
            <li key={href}>
              <Link
                href={href}
                className={cn(
                  "flex flex-col items-center rounded-md px-3 py-1 text-xs font-medium transition-colors",
                  active
                    ? "text-primary"
                    : "text-muted-foreground hover:text-foreground"
                )}
              >
                {t(labelKey)}
              </Link>
            </li>
          );
        })}
      </ul>
    </nav>
  );
}
