import { createHash } from "node:crypto";
import { Content, isContentType } from "./types";
import { isPracticalCategory, practicalContentHash } from "./practicalLesson";

type SimilarityMetadata = Pick<Content, "similarityGroupId" | "sourceId" | "sourceCitation">;
const storedContentHashes = new WeakMap<Content, string>();

/**
 * Associates the checksum of the raw Firestore document with its normalized
 * in-memory Content object. WeakMap metadata is never enumerable or serialized
 * into the callable response sent to Android.
 */
export function rememberStoredContentHash(content: Content, hash: string): void {
  storedContentHashes.set(content, hash);
}

/** Explicit editorial concept groups take priority over legacy passage metadata. */
function isPreciseSourceCitation(citation: string): boolean {
  const normalized = citation.trim();
  return normalized.length > 0 && normalized.length <= 200 &&
    /(?:\b(?:page|p\.|section|sec\.|chapter|ch\.|verse|verses|hymn|passage)\s*[\w.-]+|§\s*[\w.-]+|\b\d+[:.]\d+\b)/i.test(normalized);
}

export function contentSimilarityKey(content: SimilarityMetadata): string | null {
  const group = content.similarityGroupId?.trim();
  if (group) return `editorial:${group}`;
  const source = content.sourceId?.trim();
  const citation = content.sourceCitation?.trim().replace(/\s+/g, " ").toLowerCase();
  return source && citation && isPreciseSourceCitation(citation)
    ? `passage:${JSON.stringify([source, citation])}`
    : null;
}

/** Mixed old/new metadata uses passage identity conservatively in either order. */
export function sharesContentSimilarity(first: SimilarityMetadata, second: SimilarityMetadata): boolean {
  const firstKey = contentSimilarityKey(first);
  if (firstKey && firstKey === contentSimilarityKey(second)) return true;
  if (first.similarityGroupId?.trim() && second.similarityGroupId?.trim()) return false;
  const firstPassage = contentSimilarityKey({ sourceId: first.sourceId, sourceCitation: first.sourceCitation });
  const secondPassage = contentSimilarityKey({ sourceId: second.sourceId, sourceCitation: second.sourceCitation });
  return firstPassage !== null && firstPassage === secondPassage;
}

/** Editorial identifiers may contain slashes; only a stable digest is used as a document ID. */
export function similarityReservationId(key: string): string {
  return createHash("sha256").update(key).digest("hex");
}

export function isEligibleForDailyDelivery(content: Content, ancientLessonSourceIds: ReadonlySet<string>): boolean {
  // Never assign a document whose `type` the app cannot render (e.g. "ritual"):
  // the assignment is recorded in history for the whole day and older app
  // builds crash on it.
  if (!isContentType(content.type)) return false;
  if (content.type !== "lesson") return true;
  const review = content.practicalReview;
  return typeof content.sourceId === "string" && ancientLessonSourceIds.has(content.sourceId) &&
    typeof content.sourcePassageId === "string" && content.sourcePassageId.length > 0 &&
    isPracticalCategory(content.primaryCategoryId) && content.category === content.primaryCategoryId &&
    content.rightsReviewRequired !== true && review !== undefined &&
    review.sourceFidelityApproved && review.scopeApproved && review.safetyApproved && review.rightsApproved &&
    review.duplicateReviewed &&
    review.contentHash === (storedContentHashes.get(content) ?? practicalContentHash(content));
}
