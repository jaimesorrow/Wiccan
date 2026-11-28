import {
  ILLUSTRATION_ORIGINAL_FIELD,
  ILLUSTRATION_URL_FIELD,
  normalizedOriginalIllustrationUrl,
  originalIllustrationReference,
  originalIllustrationViolations,
} from "../originalIllustration";

const original = (suffix: string) => ({
  illustrationUrl: `https://cdn.example.org/originals/${suffix}.png`,
  illustrationOriginal: true as const,
});

describe("original illustration publication gate", () => {
  it("exports the schema field names the image backfill must use", () => {
    expect(ILLUSTRATION_URL_FIELD).toBe("illustrationUrl");
    expect(ILLUSTRATION_ORIGINAL_FIELD).toBe("illustrationOriginal");
  });

  it("accepts a canonical HTTPS URL marked as original and unused", () => {
    expect(originalIllustrationViolations(original("hearth-ash"))).toEqual([]);
    expect(normalizedOriginalIllustrationUrl(original("hearth-ash").illustrationUrl))
      .toBe("https://cdn.example.org/originals/hearth-ash.png");
    expect(originalIllustrationReference(original("hearth-ash")))
      .toBe("url:https://cdn.example.org/originals/hearth-ash.png");
  });

  it("rejects a missing, non-HTTPS, credentialed, or padded URL", () => {
    expect(originalIllustrationViolations({ illustrationOriginal: true }).join(" ")).toMatch(/illustrationUrl/);
    expect(originalIllustrationViolations({ illustrationUrl: "http://cdn.example.org/a.png", illustrationOriginal: true }).join(" ")).toMatch(/illustrationUrl/);
    expect(originalIllustrationViolations({ illustrationUrl: "https://user:pass@cdn.example.org/a.png", illustrationOriginal: true }).join(" ")).toMatch(/illustrationUrl/);
    expect(originalIllustrationViolations({ illustrationUrl: "  https://cdn.example.org/a.png  ", illustrationOriginal: true }).join(" ")).toMatch(/illustrationUrl/);
  });

  it("rejects an image that is not flagged as original", () => {
    expect(originalIllustrationViolations({
      illustrationUrl: "https://cdn.example.org/originals/hearth-ash.png",
    }).join(" ")).toMatch(/illustrationOriginal=true/);
    expect(originalIllustrationViolations({
      illustrationUrl: "https://cdn.example.org/originals/hearth-ash.png",
      illustrationOriginal: false,
    }).join(" ")).toMatch(/illustrationOriginal=true/);
  });

  it("rejects an image already used by another item, but not the item's own URL", () => {
    const inventory = [
      { id: "lesson-a", fields: original("shared") },
      { id: "lesson-b", fields: original("other") },
    ];
    expect(originalIllustrationViolations(original("shared"), inventory, "new-draft").join(" "))
      .toMatch(/already used by 'lesson-a'/);
    expect(originalIllustrationViolations(original("shared"), inventory, "lesson-a")).toEqual([]);
  });
});
