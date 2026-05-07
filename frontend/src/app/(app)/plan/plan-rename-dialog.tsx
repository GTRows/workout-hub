"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

type Props = {
  initialName: string;
  onSubmit: (newName: string) => void;
  onCancel: () => void;
  pending?: boolean;
};

export function PlanRenameDialog({
  initialName,
  onSubmit,
  onCancel,
  pending,
}: Props) {
  const t = useTranslations("plan");
  const [value, setValue] = useState(initialName);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const trimmed = value.trim();
    if (!trimmed) {
      setError(t("nameRequired"));
      return;
    }
    setError(null);
    onSubmit(trimmed);
  };

  return (
    <form
      onSubmit={handleSubmit}
      className="space-y-2 rounded-md border border-border bg-muted/30 p-3"
      data-testid="plan-rename-dialog"
    >
      <Label htmlFor="plan-rename-input" className="text-sm font-medium">
        {t("renameDialogTitle")}
      </Label>
      <Input
        id="plan-rename-input"
        value={value}
        onChange={(e) => setValue(e.target.value)}
        autoFocus
        disabled={pending}
      />
      {error && <p className="text-xs text-destructive">{error}</p>}
      <div className="flex gap-2">
        <Button type="submit" size="sm" disabled={pending}>
          {t("dialogSave")}
        </Button>
        <Button
          type="button"
          variant="ghost"
          size="sm"
          onClick={onCancel}
          disabled={pending}
        >
          {t("dialogCancel")}
        </Button>
      </div>
    </form>
  );
}
