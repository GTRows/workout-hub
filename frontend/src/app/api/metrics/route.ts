import { getRegistry } from "@/lib/metrics/registry";

export const dynamic = "force-dynamic";
export const runtime = "nodejs";

const startTime = Date.now();

export function GET() {
  const memUsage = process.memoryUsage();
  const uptimeSec = process.uptime();
  const lines = [
    "# HELP nodejs_process_uptime_seconds Process uptime in seconds.",
    "# TYPE nodejs_process_uptime_seconds gauge",
    `nodejs_process_uptime_seconds ${uptimeSec.toFixed(3)}`,
    "# HELP nodejs_process_resident_memory_bytes Resident set size in bytes.",
    "# TYPE nodejs_process_resident_memory_bytes gauge",
    `nodejs_process_resident_memory_bytes ${memUsage.rss}`,
    "# HELP nodejs_process_heap_used_bytes V8 heap used in bytes.",
    "# TYPE nodejs_process_heap_used_bytes gauge",
    `nodejs_process_heap_used_bytes ${memUsage.heapUsed}`,
    "# HELP nodejs_process_heap_total_bytes V8 heap allocated in bytes.",
    "# TYPE nodejs_process_heap_total_bytes gauge",
    `nodejs_process_heap_total_bytes ${memUsage.heapTotal}`,
    "# HELP nodejs_process_external_memory_bytes External memory in bytes.",
    "# TYPE nodejs_process_external_memory_bytes gauge",
    `nodejs_process_external_memory_bytes ${memUsage.external}`,
    "# HELP workouthub_frontend_start_time_seconds Unix timestamp when the process started.",
    "# TYPE workouthub_frontend_start_time_seconds gauge",
    `workouthub_frontend_start_time_seconds ${(startTime / 1000).toFixed(3)}`,
  ];
  const body = lines.join("\n") + "\n" + getRegistry().renderProm();
  return new Response(body, {
    status: 200,
    headers: { "content-type": "text/plain; version=0.0.4; charset=utf-8" },
  });
}
