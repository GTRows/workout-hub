import type { ReactNode } from "react";
import { BottomNav } from "@/components/bottom-nav";
import { Nav } from "@/components/nav";
import { SessionExpiredShell } from "@/lib/auth/session-expired-shell";

export default function AppLayout({ children }: { children: ReactNode }) {
  return (
    <div className="flex min-h-screen flex-col">
      <SessionExpiredShell />
      <Nav />
      <main className="mx-auto w-full max-w-6xl flex-1 px-4 py-6 pb-20 sm:pb-6">
        {children}
      </main>
      <BottomNav />
    </div>
  );
}
