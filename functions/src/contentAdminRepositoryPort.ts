import { ContentStatus, ContentTier, ContentType } from "./types";
import { GeneratedDraftFields } from "./draftGenerationPort";

export interface NewDraftContent extends GeneratedDraftFields {
  type: ContentType;
  tier: ContentTier;
  tradition: string;
  sourceId: string;
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

  publish(contentId: string): Promise<void>;
}
