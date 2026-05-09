import { z } from "zod";
import { getRegistry } from "@/lib/metrics/registry";
import { normalizeRoute } from "@/lib/metrics/route-label";

export const dynamic = "force-dynamic";
export const runtime = "nodejs";

const VitalSchema = z.object({
  name: z.enum(["LCP", "CLS", "INP", "FCP", "TTFB"]),
  value: z.number().finite().nonnegative(),
  id: z.string().min(1).max(128),
  navigationType: z
    .enum(["navigate", "reload", "back-forward", "back-forward-cache", "prerender"])
    .optional(),
  route: z.string().min(1).max(256),
});

function badRequest(): Response {
  return new Response(JSON.stringify({ error: "invalid payload" }), {
    status: 400,
    headers: { "content-type": "application/json" },
  });
}

export async function POST(req: Request): Promise<Response> {
  let body: unknown;
  try {
    body = await req.json();
  } catch {
    return badRequest();
  }
  const parsed = VitalSchema.safeParse(body);
  if (!parsed.success) {
    return badRequest();
  }
  const { name, value, route } = parsed.data;
  const recorded = name === "CLS" ? value : value / 1000;
  getRegistry().recordWebVital(name, normalizeRoute(route), recorded);
  return new Response(null, { status: 204 });
}
