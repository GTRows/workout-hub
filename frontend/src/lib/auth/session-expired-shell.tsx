"use client";

import { useSessionExpiredRedirect } from "@/lib/auth/use-session-expired-redirect";

export function SessionExpiredShell(): null {
  useSessionExpiredRedirect();
  return null;
}
