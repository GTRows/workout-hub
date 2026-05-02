export const dynamic = "force-dynamic";
export const runtime = "nodejs";

export function GET() {
  return new Response(
    JSON.stringify({ status: "alive" }),
    { status: 200, headers: { "content-type": "application/json" } }
  );
}
