import type { PracticalLessonFields } from "./practicalLesson";
import type { SourcePassage } from "./sourcePassages";
import { ContentCategory, ContentType } from "./types";

/** The fields an LLM generates for one draft; everything else (type, tier, tradition, sourceId, status) is assigned by generateDraft itself. */
export interface GeneratedDraftFields extends PracticalLessonFields {
  title: string;
  intro: string;
  body: string;
  /** Separate structured fields on new drafts; optional for older stored drafts. */
  reflection?: string | null;
  practice?: string | null;
  materials: string[] | null;
  safetyNote: string | null;
  tags: string[];
  /** The single Grimoire category this piece best fits, chosen by the generator. */
  category: ContentCategory;
}

export interface DraftGenerationRequest {
  type: ContentType;
  tradition: string;
  period: string;
  sourceTitle: string;
  sourceReference: string;
  /** The vetted source summary — independently written, never a verbatim excerpt. */
  summary: string;
  /** The factual boundary for historical claims in generated lessons. */
  groundedFacts: string[];
  /** Exact editor-inspected passage, never a source inferred after generation. */
  passage?: SourcePassage;
  /**
   * Titles of every existing content document (published and draft) plus
   * candidates already rejected or accepted in this batch. The model must not
   * reuse or lightly reword any of them.
   */
  existingTitles?: string[];
  /** Compact concept summaries the model must not reproduce or merely rename. */
  excludedConcepts?: Array<{
    id: string;
    title: string;
    primaryMethod: string;
    intendedPurpose: string;
    ritualActions: string[];
    followUpAction: string;
  }>;
}

/**
 * Port between the generation logic and the LLM. Kept as an interface so
 * generateDraft can be unit tested against a fake instead of making real
 * (paid) API calls.
 */
export interface DraftGenerationPort {
  generateDraft(request: DraftGenerationRequest): Promise<GeneratedDraftFields>;
}
