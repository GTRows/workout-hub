"use client";

import { useEffect } from "react";

type Props = {
  message: string;
  onDismiss: () => void;
};

export function PrToast({ message, onDismiss }: Props) {
  useEffect(() => {
    const id = window.setTimeout(onDismiss, 3000);
    return () => window.clearTimeout(id);
  }, [onDismiss]);

  return (
    <div
      role="status"
      aria-live="polite"
      data-testid="pr-toast"
      className="pointer-events-none fixed inset-x-0 bottom-24 z-50 flex justify-center px-4 sm:bottom-6"
    >
      <div className="pointer-events-auto rounded-full bg-primary px-5 py-2 text-sm font-medium text-primary-foreground shadow-lg transition-transform animate-in fade-in scale-105">
        {message}
      </div>
    </div>
  );
}
