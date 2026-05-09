// Route-shaped pulse placeholders. One file, one switch — keeps surface tight.
"use client";

import { useTranslations } from "next-intl";
import type { ReactElement } from "react";
import { Skeleton } from "@/components/ui/skeleton";

export type RouteSkeletonVariant =
  | "dashboard"
  | "plan"
  | "session"
  | "history"
  | "exercises"
  | "exercise-detail"
  | "metrics"
  | "profile"
  | "insights"
  | "prs"
  | "achievements"
  | "nutrition"
  | "export"
  | "export-ai"
  | "export-ai-apply";

export interface RouteSkeletonProps {
  variant: RouteSkeletonVariant;
}

export function RouteSkeleton({ variant }: RouteSkeletonProps): ReactElement {
  const t = useTranslations("common");
  return (
    <div
      role="status"
      aria-busy="true"
      aria-label={t("loading")}
      className="space-y-3"
      data-testid={`route-skeleton-${variant}`}
    >
      {renderVariant(variant)}
    </div>
  );
}

function renderVariant(variant: RouteSkeletonVariant): ReactElement {
  switch (variant) {
    case "dashboard":
      return (
        <>
          <Skeleton className="h-7 w-1/2" />
          <Skeleton className="h-32 w-full" />
          <Skeleton className="h-20 w-full" />
          <Skeleton className="h-20 w-full" />
        </>
      );
    case "plan":
      return (
        <>
          <Skeleton className="h-7 w-1/3" />
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-7">
            {arr(7).map((i) => (
              <Skeleton key={i} className="h-24 w-full" />
            ))}
          </div>
        </>
      );
    case "session":
      return (
        <>
          <Skeleton className="h-7 w-2/3" />
          {arr(4).map((i) => (
            <Skeleton key={i} className="h-24 w-full" />
          ))}
        </>
      );
    case "history":
      return (
        <>
          <Skeleton className="h-7 w-1/3" />
          <Skeleton className="h-64 w-full" />
          {arr(3).map((i) => (
            <Skeleton key={i} className="h-12 w-full" />
          ))}
        </>
      );
    case "exercises":
      return (
        <>
          <Skeleton className="h-7 w-1/3" />
          <Skeleton className="h-10 w-full" />
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
            {arr(6).map((i) => (
              <Skeleton key={i} className="h-32 w-full" />
            ))}
          </div>
        </>
      );
    case "exercise-detail":
      return (
        <>
          <Skeleton className="h-7 w-1/2" />
          <Skeleton className="h-48 w-full" />
          <Skeleton className="h-6 w-1/3" />
          <Skeleton className="h-6 w-1/3" />
          <Skeleton className="h-6 w-1/3" />
        </>
      );
    case "metrics":
      return (
        <>
          <Skeleton className="h-7 w-1/3" />
          <Skeleton className="h-48 w-full" />
          <Skeleton className="h-48 w-full" />
          <Skeleton className="h-10 w-full" />
        </>
      );
    case "profile":
      return (
        <>
          <Skeleton className="h-7 w-1/3" />
          {arr(6).map((i) => (
            <div key={i} className="space-y-2">
              <Skeleton className="h-4 w-1/3" />
              <Skeleton className="h-10 w-full" />
            </div>
          ))}
        </>
      );
    case "insights":
      return (
        <>
          <Skeleton className="h-7 w-1/3" />
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
            {arr(3).map((i) => (
              <Skeleton key={i} className="h-20 w-full" />
            ))}
          </div>
          <Skeleton className="h-48 w-full" />
        </>
      );
    case "prs":
      return (
        <>
          <Skeleton className="h-7 w-1/3" />
          {arr(5).map((i) => (
            <Skeleton key={i} className="h-16 w-full" />
          ))}
        </>
      );
    case "achievements":
      return (
        <>
          <Skeleton className="h-7 w-1/3" />
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            {arr(6).map((i) => (
              <Skeleton key={i} className="h-24 w-full" />
            ))}
          </div>
        </>
      );
    case "nutrition":
      return (
        <>
          <Skeleton className="h-7 w-1/3" />
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
            {arr(3).map((i) => (
              <Skeleton key={i} className="h-20 w-full" />
            ))}
          </div>
          <Skeleton className="h-32 w-full" />
        </>
      );
    case "export":
      return (
        <>
          <Skeleton className="h-7 w-1/3" />
          <Skeleton className="h-32 w-full" />
          <Skeleton className="h-32 w-full" />
          <Skeleton className="h-24 w-full" />
        </>
      );
    case "export-ai":
      return (
        <>
          <Skeleton className="h-7 w-1/3" />
          <Skeleton className="h-48 w-full" />
        </>
      );
    case "export-ai-apply":
      return (
        <>
          <Skeleton className="h-7 w-1/3" />
          <Skeleton className="h-48 w-full" />
          <Skeleton className="h-10 w-full" />
        </>
      );
    default:
      return assertNever(variant);
  }
}

function arr(n: number): number[] {
  return Array.from({ length: n }, (_, i) => i);
}

function assertNever(x: never): never {
  throw new Error("Unhandled RouteSkeleton variant: " + String(x));
}
