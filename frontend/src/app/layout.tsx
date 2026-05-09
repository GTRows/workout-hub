import type { Metadata, Viewport } from "next";
import { getLocale, getMessages } from "next-intl/server";
import type { ReactNode } from "react";
import { Providers } from "@/components/providers";
import { ServiceWorkerRegistrar } from "@/components/service-worker-registrar";
import { WebVitalsReporter } from "@/components/web-vitals-reporter";
import "./globals.css";

export const metadata: Metadata = {
  title: "WorkoutHub",
  description: "Self-hosted multi-user fitness tracker",
  manifest: "/manifest.webmanifest",
  icons: {
    icon: [
      { url: "/icons/icon-192.png", sizes: "192x192", type: "image/png" },
      { url: "/icons/icon-512.png", sizes: "512x512", type: "image/png" },
    ],
    apple: [{ url: "/icons/apple-touch-icon.png", sizes: "180x180" }],
  },
  appleWebApp: {
    capable: true,
    statusBarStyle: "default",
    title: "WorkoutHub",
  },
};

export const viewport: Viewport = {
  themeColor: "#0ea5e9",
  width: "device-width",
  initialScale: 1,
};

export default async function RootLayout({ children }: { children: ReactNode }) {
  const locale = await getLocale();
  const messages = await getMessages();

  return (
    <html lang={locale}>
      <body className="min-h-screen bg-background text-foreground antialiased">
        <Providers locale={locale} messages={messages}>
          {children}
        </Providers>
        <ServiceWorkerRegistrar />
        <WebVitalsReporter />
      </body>
    </html>
  );
}
