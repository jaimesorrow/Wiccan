import { ContentStatus, ContentTier, ContentType } from "./types";
import { GeneratedDraftFields } from "./draftGenerationPort";

export interface NewDraftContent extends GeneratedDraftFields {
  type: ContentType;
  tier: ContentTier;
  tradition: string;
  sourceId: string;
  /** A seeded/editorial draft cannot be published until its rights review is cleared. */
  rightsReviewRequired?: boolean;
}

export interface StoredContent extends NewDraftContent {
  status: ContentStatus;
}

/**
 * Port between the generation/approval logic and the `content` collection's
 * admin-only writes (draft creation, status transitions). Never used for
 * the read-side selection logic — that stays in ContentRepositoryPort.
 */
export interface ContentAdminRepositoryPort {
  /** Always written with status "draft". Returns the new document's id. */
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
