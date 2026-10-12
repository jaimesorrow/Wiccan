import { ContentStatus, ContentTier, ContentType } from "./types";
import { GeneratedDraftFields } from "./draftGenerationPort";

export interface NewDraftContent extends GeneratedDraftFields {
  type: ContentType;
  tier: ContentTier;
  tradition: string;
  sourceId: string;
  sourceCitation?: string;
  sourceUrl?: string;
  rightsReviewRequired?: boolean;
  rightsNotes?: string | null;
  /** Editor assigned; carry the existing group when revising or rewording a lesson. */
  similarityGroupId?: string;
}

export interface StoredContent extends NewDraftContent {
  status: ContentStatus;
  /** Server-written audit metadata. Older hand-authored drafts may not carry it. */
  generatedBy?: string;
  generationSourceKind?: "ancient-grimoire";
  generatedAt?: unknown;
}

/**
 * Port between the generation/approval logic and the `content` collection's
 * admin-only writes (draft creation, status transitions). Never used for
 * the read-side selection logic — that stays in ContentRepositoryPort.
 */
export interface ContentAdminRepositoryPort {
  /**
   * Always written as a rights-review-required draft with immutable generator,
   * source-kind, and server-timestamp audit metadata. Returns the new document's id.
   */
  createDraft(draft: NewDraftContent): Promise<string>;

  getStatus(contentId: string): Promise<ContentStatus | null>;

  /** Full stored document, for the pre-publish safety/completeness check in approveDraft. */
  getContent(contentId: string): Promise<StoredContent | null>;

  publish(contentId: string): Promise<void>;

  /**
   * True if any existing document (draft or published) already has this
   * exact title, compared case- and whitespace-insensitively - a guard
   * against a generation batch silently piling up duplicate-titled twins.
   */
  titleExists(title: string): Promise<boolean>;
}
