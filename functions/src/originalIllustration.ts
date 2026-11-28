/**
 * Publication gate for an item's own original lesson-related image.
 *
 * Schema fields (backfill and new drafts must use these exact names):
 * - illustrationUrl: canonical HTTPS URL of this item's unique original image
 * - illustrationOriginal: true only when that image was created for this item
 *   and is not copied from a reference photo, historical scan, or another item
 *
 * Existing published items may omit these fields until the staged backfill
 * attaches a unique original image. Delivery and the Android app keep
 * illustrationUrl optional and fall back to bundled category artwork, so
 * already-published lessons stay readable during backfill. The gate below
 * applies only when a draft is approved or published.
 */
export const ILLUSTRATION_URL_FIELD = "illustrationUrl" as const;
export const ILLUSTRATION_ORIGINAL_FIELD = "illustrationOriginal" as const;

export interface OriginalIllustrationFields {
  illustrationUrl?: unknown;
  illustrationOriginal?: unknown;
}

export function normalizedOriginalIllustrationUrl(value: unknown): string | null {
  if (typeof value !== "string" || value.trim() !== value || value.length === 0) return null;
  try {
    const url = new URL(value);
    if (url.protocol !== "https:" || !url.hostname || url.username || url.password) return null;
    return url.toString();
  } catch {
    return null;
  }
}

export function originalIllustrationReference(fields: OriginalIllustrationFields): string | null {
  const url = normalizedOriginalIllustrationUrl(fields.illustrationUrl);
  return url ? `url:${url}` : null;
}

/**
 * A draft cannot be approved or published without its own original image,
 * and that image cannot be reused by any other draft or published item.
 */
export function originalIllustrationViolations(
  fields: OriginalIllustrationFields,
  inventory: Array<{ id: string; fields: OriginalIllustrationFields }> = [],
  contentId?: string,
): string[] {
  const violations: string[] = [];
  if (fields.illustrationOriginal !== true) {
    violations.push(
      "publication requires illustrationOriginal=true (an original image created for this item, not reused or copied).",
    );
  }
  const url = normalizedOriginalIllustrationUrl(fields.illustrationUrl);
  if (url === null) {
    violations.push(
      "publication requires a unique HTTPS illustrationUrl for this item's own original image.",
    );
    return violations;
  }
  const reused = inventory.find((record) => {
    if (contentId && record.id === contentId) return false;
    return normalizedOriginalIllustrationUrl(record.fields.illustrationUrl) === url;
  });
  if (reused) {
    violations.push(
      `illustrationUrl is already used by '${reused.id}'; each lesson, spell and potion must have its own original image.`,
    );
  }
  return violations;
}
