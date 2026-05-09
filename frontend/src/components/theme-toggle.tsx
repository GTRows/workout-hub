"use client";

import { useEffect, useState } from "react";
import { Button } from "@/components/ui/button";

const STORAGE_KEY = "wh.theme";

type Theme = "light" | "dark" | null;

function readStored(): Theme {
  if (typeof window === "undefined") return null;
  const v = window.localStorage.getItem(STORAGE_KEY);
  return v === "light" || v === "dark" ? v : null;
}

function applyTheme(theme: Theme) {
  if (typeof document === "undefined") return;
  const root = document.documentElement;
  root.classList.remove("dark", "light");
  if (theme) root.classList.add(theme);
}

export function ThemeToggle() {
  const [theme, setTheme] = useState<Theme>(null);

  useEffect(() => {
    const stored = readStored();
    // Reads platform localStorage (browser-only). External-sync pattern
    // that eslint-plugin-react-hooks@7's set-state-in-effect does not model.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setTheme(stored);
    applyTheme(stored);
  }, []);

  const cycle = () => {
    const next: Theme =
      theme === null ? "dark" : theme === "dark" ? "light" : null;
    setTheme(next);
    if (next) {
      window.localStorage.setItem(STORAGE_KEY, next);
    } else {
      window.localStorage.removeItem(STORAGE_KEY);
    }
    applyTheme(next);
  };

  const label =
    theme === "dark" ? "Dark" : theme === "light" ? "Light" : "Auto";

  return (
    <Button
      variant="outline"
      size="sm"
      onClick={cycle}
      data-testid="theme-toggle"
      aria-label={`Theme: ${label}, click to cycle`}
    >
      {label}
    </Button>
  );
}
