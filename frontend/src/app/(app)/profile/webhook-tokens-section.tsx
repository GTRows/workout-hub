"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Card, CardDescription, CardTitle } from "@/components/ui/card";
import { PrToast } from "@/components/pr-toast";
import {
  fetchWebhookTokens,
  mintWebhookToken,
  revokeWebhookToken,
} from "@/lib/api/endpoints";

function buildScaleUrl(token: string): string {
  if (typeof window === "undefined") return token;
  return `${window.location.origin}/api/webhooks/scale/${token}`;
}

export function WebhookTokensSection() {
  const t = useTranslations("webhookTokens");
  const tProfile = useTranslations("profile");
  const qc = useQueryClient();

  const [toastMessage, setToastMessage] = useState<string | null>(null);

  const listQuery = useQuery({
    queryKey: ["webhook-tokens", "scale"],
    queryFn: () => fetchWebhookTokens("scale"),
  });

  const mintMutation = useMutation({
    mutationFn: () => mintWebhookToken("scale"),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["webhook-tokens"] }),
    onError: () => setToastMessage(t("toastMintError")),
  });

  const revokeMutation = useMutation({
    mutationFn: (id: string) => revokeWebhookToken(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["webhook-tokens"] }),
    onError: () => setToastMessage(t("toastRevokeError")),
  });

  return (
    <Card className="space-y-4" data-testid="webhook-tokens-card">
      <div className="space-y-1">
        <CardTitle className="text-lg">{t("title")}</CardTitle>
        <CardDescription>{t("description")}</CardDescription>
      </div>
      {listQuery.isError && (
        <div className="space-y-2">
          <p role="alert" className="text-sm text-destructive">
            {t("loadError")}
          </p>
          <Button variant="outline" size="sm" onClick={() => listQuery.refetch()}>
            {tProfile("retry")}
          </Button>
        </div>
      )}
      <div>
        <Button
          variant="outline"
          size="sm"
          onClick={() => mintMutation.mutate()}
          disabled={mintMutation.isPending}
          data-testid="mint-token"
        >
          {mintMutation.isPending ? t("minting") : t("mint")}
        </Button>
      </div>
      {toastMessage && (
        <PrToast
          message={toastMessage}
          onDismiss={() => setToastMessage(null)}
        />
      )}
      {listQuery.data && listQuery.data.length > 0 && (
        <ul className="space-y-2 text-sm">
          {listQuery.data.map((tk) => (
            <li
              key={tk.id}
              className="flex items-start justify-between gap-2 border-t border-border pt-2"
            >
              <div className="space-y-1 break-all">
                <p className="font-mono text-xs" data-testid={`token-url-${tk.id}`}>
                  {buildScaleUrl(tk.token)}
                </p>
                <p className="text-xs text-muted-foreground">
                  {tk.lastUsedAt
                    ? t("lastUsed", { time: new Date(tk.lastUsedAt).toLocaleString() })
                    : t("never")}
                </p>
              </div>
              <Button
                variant="outline"
                size="sm"
                onClick={() => {
                  if (confirm(t("revokeConfirm"))) revokeMutation.mutate(tk.id);
                }}
                data-testid={`revoke-${tk.id}`}
              >
                {t("revoke")}
              </Button>
            </li>
          ))}
        </ul>
      )}
    </Card>
  );
}
