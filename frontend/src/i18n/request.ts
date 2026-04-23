import { getRequestConfig } from "next-intl/server";

const SUPPORTED = ["tr", "en"] as const;
type Locale = (typeof SUPPORTED)[number];
const DEFAULT_LOCALE: Locale =
  (process.env.NEXT_PUBLIC_DEFAULT_LOCALE as Locale | undefined) ?? "tr";

function resolveLocale(raw: string | undefined): Locale {
  if (raw && SUPPORTED.includes(raw as Locale)) return raw as Locale;
  return DEFAULT_LOCALE;
}

export default getRequestConfig(async ({ requestLocale }) => {
  const locale = resolveLocale(await requestLocale);
  const messages = (await import(`../../messages/${locale}.json`)).default;
  return { locale, messages };
});
