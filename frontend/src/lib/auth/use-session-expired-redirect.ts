"use client";

import { useEffect } from "react";
import { usePathname, useRouter } from "next/navigation";
import { getAccessToken, subscribeAccessToken } from "@/lib/auth/token-store";

/**
 * Shell-level subscriber that redirects to /login when the refresh interceptor
 * silently clears tokens (see client.ts refreshAccessToken). Manual logout
 * already navigates to /login before clearing tokens, so the
 * pathname.startsWith("/login") guard short-circuits that path.
 */
export function useSessionExpiredRedirect(): void {
  const router = useRouter();
  const pathname = usePathname();

  useEffect(() => {
    let lastSeen: string | null = getAccessToken();
    const unsubscribe = subscribeAccessToken((next) => {
      const previous = lastSeen;
      lastSeen = next;
      if (next !== null) return;
      if (previous === null) return;
      if (pathname.startsWith("/login")) return;
      router.replace(
        `/login?reason=session-expired&next=${encodeURIComponent(pathname)}`
      );
    });
    return unsubscribe;
  }, [router, pathname]);
}
