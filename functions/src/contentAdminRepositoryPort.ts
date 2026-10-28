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
  createdAt?: unknown;
  updatedAt?: unknown;
  /** Server-written audit metadata. Older hand-authored drafts may not carry it. */
  generatedBy?: string;
  generationSourceKind?: "historical-grimoire";
  generatedAt?: unknown;
}

export interface VersionedStoredContent {
  content: StoredContent;
  /** Firestore update time encoded as seconds:nanoseconds. */
  updateTime: string;
}

export interface PublicationRequest {
  contentId: string;
  expectedUpdateTime: string;
  expectedContentHash: string;
  operator: string;
  approvalDigest: string;
}

export interface PublicationResult {
  contentId: string;
  outcome: "published" | "already-published";
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

  /** Read the document together with the version that an approval is bound to. */
  getVersionedContent(contentId: string): Promise<VersionedStoredContent | null>;

  /** Atomically revalidate and publish a reviewed batch, including immutable audit rows. */
  publishBatch(requests: PublicationRequest[]): Promise<PublicationResult[]>;

  /**
   * True if any existing document (draft or published) already has this
   * exact title, compared case- and whitespace-insensitively - a guard
   * against a generation batch silently piling up duplicate-titled twins.
   */
  titleExists(title: string): Promise<boolean>;
}
