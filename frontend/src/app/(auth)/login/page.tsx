"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { useRouter, useSearchParams } from "next/navigation";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { ApiError } from "@/lib/api/client";
import { login as loginEndpoint } from "@/lib/api/endpoints";
import { setAccessToken, setRefreshToken } from "@/lib/auth/token-store";
import { loginFormSchema, type LoginFormValues } from "@/lib/auth/login-schema";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

export default function LoginPage() {
  const t = useTranslations("auth");
  const router = useRouter();
  const searchParams = useSearchParams();
  const [serverError, setServerError] = useState<string | null>(null);

  const form = useForm<LoginFormValues>({
    resolver: zodResolver(loginFormSchema),
    defaultValues: { email: "", password: "" },
    mode: "onBlur",
  });

  const mutation = useMutation({
    mutationFn: (values: LoginFormValues) =>
      loginEndpoint(values.email, values.password),
    onSuccess: (data) => {
      setAccessToken(data.accessToken);
      setRefreshToken(data.refreshToken);
      const next = searchParams.get("next") ?? "/dashboard";
      router.replace(next);
    },
    onError: (error: unknown) => {
      if (error instanceof ApiError && (error.status === 401 || error.status === 400)) {
        setServerError(t("invalidCredentials"));
      } else {
        setServerError("Unexpected error. Please try again.");
      }
    },
  });

  const onSubmit = form.handleSubmit((values) => {
    setServerError(null);
    mutation.mutate(values);
  });

  return (
    <form
      onSubmit={onSubmit}
      className="space-y-5 rounded-lg border border-border bg-background p-6 shadow-sm"
      noValidate
    >
      <h1 className="text-xl font-semibold">{t("loginTitle")}</h1>

      <div className="space-y-2">
        <Label htmlFor="email">{t("email")}</Label>
        <Input
          id="email"
          type="email"
          autoComplete="email"
          aria-invalid={!!form.formState.errors.email || undefined}
          {...form.register("email")}
        />
        {form.formState.errors.email?.message && (
          <p role="alert" className="text-xs text-destructive">
            {form.formState.errors.email.message}
          </p>
        )}
      </div>

      <div className="space-y-2">
        <Label htmlFor="password">{t("password")}</Label>
        <Input
          id="password"
          type="password"
          autoComplete="current-password"
          aria-invalid={!!form.formState.errors.password || undefined}
          {...form.register("password")}
        />
        {form.formState.errors.password?.message && (
          <p role="alert" className="text-xs text-destructive">
            {form.formState.errors.password.message}
          </p>
        )}
      </div>

      {serverError && (
        <p role="alert" className="text-sm text-destructive">
          {serverError}
        </p>
      )}

      <Button type="submit" disabled={mutation.isPending} className="w-full">
        {t("submit")}
      </Button>
    </form>
  );
}
