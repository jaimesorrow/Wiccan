export type SupportedLocale = "en" | "es" | "pt-BR";

/** Canonicalizes a bounded client locale. Unsupported or absent values fall back to English. */
export function canonicalizeLocale(value: unknown): SupportedLocale {
  if (typeof value !== "string" || value.length > 35) return "en";
  const normalized = value.trim().replaceAll("_", "-").toLowerCase();
  if (normalized === "es" || normalized.startsWith("es-")) return "es";
  if (normalized === "pt-br") return "pt-BR";
  return "en";
}
