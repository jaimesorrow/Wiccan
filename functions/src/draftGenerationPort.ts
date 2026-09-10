import { ContentType } from "./types";

/** The fields an LLM generates for one draft; everything else (type, tier, tradition, sourceId, status) is assigned by generateDraft itself. */
export interface GeneratedDraftFields {
  title: string;
  intro: string;
  body: string;
  materials: string[] | null;
  safetyNote: string | null;
  tags: string[];
}

export interface DraftGenerationRequest {
  type: ContentType;
  tradition: string;
  /** The vetted source summary — inspiration only, never to be echoed. */
  summary: string;
}

/**
 * Port between the generation logic and the LLM. Kept as an interface so
 * generateDraft can be unit tested against a fake instead of making real
 * (paid) API calls.
 */
export interface DraftGenerationPort {
  generateDraft(request: DraftGenerationRequest): Promise<GeneratedDraftFields>;
}
