import { canonicalizeLocale } from "../locale";

describe("canonicalizeLocale", () => {
  it.each([
    ["es", "es"],
    ["es-MX", "es"],
    ["es_ES", "es"],
    ["pt-BR", "pt-BR"],
    ["pt_BR", "pt-BR"],
  ])("canonicalizes %s to %s", (input, expected) => {
    expect(canonicalizeLocale(input)).toBe(expected);
  });

  it.each([undefined, null, "", "fr", "pt-PT", 42, "x".repeat(36)])(
    "falls back to English for unsupported input %p",
    (input) => expect(canonicalizeLocale(input)).toBe("en"),
  );
});
