import type { ContentStatus } from "./types";

export const REVIEWABLE_BUNDLED_ILLUSTRATION_KEYS = [
  "botanical",
  "candle",
  "cord",
  "cord-knot",
  "grimoire",
  "herbal",
  "knot-work",
  "lunar",
  "moon",
  "moon-phases",
  "ritual",
] as const;

export interface IllustrationFields {
  illustrationUrl?: unknown;
  illustrationKey?: unknown;
  illustrationKeyReviewed?: unknown;
}

export interface IllustrationAuditDocument {
  id: string;
  fields: IllustrationFields & { status?: ContentStatus | string; contentStatus?: string; title?: unknown };
}

export function normalizedHttpsIllustrationUrl(value: unknown): string | null {
  if (typeof value !== "string" || value.trim() !== value || value.length === 0) return null;
  try {
    const url = new URL(value);
    if (url.protocol !== "https:" || !url.hostname || url.username || url.password) return null;
    return url.toString();
  } catch {
    return null;
  }
}

export function normalizedBundledIllustrationKey(value: unknown): string | null {
  if (typeof value !== "string") return null;
  const key = value.trim().toLowerCase();
  return (REVIEWABLE_BUNDLED_ILLUSTRATION_KEYS as readonly string[]).includes(key) ? key : null;
}

/** Publication requires remote HTTPS artwork or an allow-listed, explicitly reviewed bundled asset. */
export function illustrationPublicationViolations(fields: IllustrationFields): string[] {
  const urlPresent = typeof fields.illustrationUrl === "string" && fields.illustrationUrl.length > 0;
  const keyPresent = typeof fields.illustrationKey === "string" && fields.illustrationKey.trim().length > 0;
  const url = normalizedHttpsIllustrationUrl(fields.illustrationUrl);
  const key = normalizedBundledIllustrationKey(fields.illustrationKey);
  const violations: string[] = [];
  if (urlPresent && url === null) violations.push("illustrationUrl must be a canonical HTTPS URL without credentials or surrounding whitespace.");
  if (keyPresent && key === null) violations.push(`illustrationKey is not an approved bundled asset identifier.`);
  if (key !== null && fields.illustrationKeyReviewed !== true) {
    violations.push("bundled illustrationKey requires illustrationKeyReviewed=true.");
  }
  if (url === null && !(key !== null && fields.illustrationKeyReviewed === true)) {
    violations.push("publication requires an HTTPS illustrationUrl or an explicitly reviewed bundled illustrationKey.");
  }
  return violations;
}

export function illustrationReference(fields: IllustrationFields): string | null {
  const url = normalizedHttpsIllustrationUrl(fields.illustrationUrl);
  if (url !== null) return `url:${url}`;
  const key = normalizedBundledIllustrationKey(fields.illustrationKey);
  if (key !== null && fields.illustrationKeyReviewed === true) return `bundle:${key}`;
  return null;
}

export function auditIllustrations(documents: IllustrationAuditDocument[]) {
  const records = documents.map(({ id, fields }) => {
    const violations = illustrationPublicationViolations(fields);
    const published = fields.status === "published" || fields.contentStatus === "published";
    return {
      id,
      title: typeof fields.title === "string" ? fields.title : null,
      status: published ? "published" : "draft",
      reference: illustrationReference(fields),
      violations,
    };
  });
  const groups = new Map<string, string[]>();
  records.forEach((record) => {
    if (record.reference === null) return;
    groups.set(record.reference, [...(groups.get(record.reference) ?? []), record.id]);
  });
  const duplicateReferences = [...groups.entries()]
    .filter(([, ids]) => ids.length > 1)
    .map(([reference, ids]) => ({ reference, contentIds: ids.sort() }))
    .sort((left, right) => left.reference.localeCompare(right.reference));
  return {
    totals: {
      documents: records.length,
      published: records.filter((record) => record.status === "published").length,
      missingOrInvalid: records.filter((record) => record.violations.length > 0).length,
      invalidPublished: records.filter((record) => record.status === "published" && record.violations.length > 0).length,
      duplicateReferences: duplicateReferences.length,
    },
    missingOrInvalid: records.filter((record) => record.violations.length > 0),
    duplicateReferences,
  };
}
