export type SupportedLocale = "en" | "es" | "pt-BR";

/** Canonicalizes a bounded client locale. Unsupported or absent values fall back to English. */
export function canonicalizeLocale(value: unknown): SupportedLocale {
  if (typeof value !== "string" || value.length > 35) return "en";
  const normalized = value.trim().replaceAll("_", "-").toLowerCase();
  if (normalized === "es" || normalized.startsWith("es-")) return "es";
  if (normalized === "pt-br") return "pt-BR";
  return "en";
}

/** Canonicalizes a present locale. Absent or empty values stay undefined so a token refresh cannot reset a saved language to English. */
export function optionalCanonicalizeLocale(value: unknown): SupportedLocale | undefined {
  if (value === undefined || value === null) return undefined;
  if (typeof value === "string" && value.trim() === "") return undefined;
  return canonicalizeLocale(value);
}
