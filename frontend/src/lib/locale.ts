/**
 * Picks the locale-appropriate side of a bilingual field, with a graceful
 * fallback to the other locale so UI never renders an empty string.
 */
export function pickLocaleField(
  locale: string,
  tr: string | null | undefined,
  en: string | null | undefined
): string {
  if (locale === "en") return en ?? tr ?? "";
  return tr ?? en ?? "";
}

export function pickLocaleArray(
  locale: string,
  tr: string[] | null | undefined,
  en: string[] | null | undefined
): string[] {
  if (locale === "en") return en ?? tr ?? [];
  return tr ?? en ?? [];
}
