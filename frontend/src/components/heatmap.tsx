"use client";

import type { HeatmapDay } from "@/lib/api/schemas";
import { cn } from "@/lib/utils";

function intensity(count: number): string {
  if (count <= 0) return "bg-muted";
  if (count === 1) return "bg-primary/40";
  if (count === 2) return "bg-primary/70";
  return "bg-primary";
}

export function Heatmap({ days }: { days: HeatmapDay[] }) {
  const columns: HeatmapDay[][] = [];
  for (let i = 0; i < days.length; i += 7) {
    columns.push(days.slice(i, i + 7));
  }

  return (
    <div
      role="grid"
      aria-label="heatmap"
      data-testid="heatmap-grid"
      className="flex gap-1"
    >
      {columns.map((week, wi) => (
        <div key={wi} className="flex flex-col gap-1" role="row">
          {week.map((d) => (
            <span
              key={d.date}
              role="gridcell"
              title={`${d.date}: ${d.sessionCount}`}
              aria-label={`${d.date} ${d.sessionCount} sessions`}
              className={cn(
                "size-3 rounded-sm ring-1 ring-inset ring-black/5",
                intensity(d.sessionCount)
              )}
            />
          ))}
        </div>
      ))}
    </div>
  );
}
