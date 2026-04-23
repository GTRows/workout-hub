export const dynamic = "force-static";

export default function OfflinePage() {
  return (
    <main className="mx-auto flex min-h-screen max-w-md flex-col items-center justify-center gap-4 p-6 text-center">
      <h1 className="text-2xl font-semibold">WorkoutHub</h1>
      <p className="text-muted-foreground">
        Baglanti yok. Internet geldiginde bu sayfa kendi kendini yeniler.
      </p>
      <p className="text-sm text-muted-foreground">
        No connection. The page will retry once you are back online.
      </p>
    </main>
  );
}
