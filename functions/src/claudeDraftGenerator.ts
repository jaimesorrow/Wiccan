import Anthropic from "@anthropic-ai/sdk";
import { z } from "zod";
import { zodOutputFormat } from "@anthropic-ai/sdk/helpers/zod";
import { DraftGenerationPort, DraftGenerationRequest, GeneratedDraftFields } from "./draftGenerationPort";
import { CONTENT_CATEGORIES } from "./types";

const MODEL = "claude-opus-5";

const GeneratedDraftSchema = z.object({
  title: z.string().min(1),
  intro: z.string().min(1),
  body: z.string().min(1),
  materials: z.array(z.string()).nullable(),
  safetyNote: z.string().nullable(),
  tags: z.array(z.string()),
  category: z.enum(CONTENT_CATEGORIES),
});

/**
 * Builds the generation prompt per the hard constraint: the source is
 * thematic inspiration only — never quoted, closely paraphrased, or
 * structurally mirrored. `summary` is Jaime's own vetted summary from
 * sources.json, never a verbatim excerpt of the original source.
 *
 * The safety rules below are non-negotiable content-policy requirements,
 * not stylistic suggestions - Wiccan ships real safety instructions to
 * real users, so a generated piece that fails them cannot be published.
 */
function buildPrompt(request: DraftGenerationRequest): string {
  return `You are drafting one original ${request.type} for Wiccan, a modern witchcraft app.
Use the following historical tradition ONLY as thematic inspiration —
do not quote, closely paraphrase, or mirror its structure or wording.
Write something new, in your own words, that captures the same
practical spirit and lineage.

Tradition: ${request.tradition}
Background (for inspiration only, not to be echoed): ${request.summary}

Choose exactly one category that best fits this piece from:
${CONTENT_CATEGORIES.join(", ")}

Safety rules - the draft must NOT:
- diagnose, treat, or claim to cure any medical or mental health condition
- present itself as a replacement for professional medical, legal, or financial care
- instruct ingestion of anything unsafe, or of any unidentified wild plant
- call for toxic, poisonous, or otherwise hazardous materials
- guarantee a specific supernatural outcome

If the piece involves open flame, smoke/incense, an allergen, an ingredient
unsafe for pets or children, or anything applied to skin, include a clear
safetyNote covering it. Set safetyNote to null only when the piece
genuinely carries none of these risks.

Output strict JSON matching this shape:
{ "title": string, "intro": string, "body": string,
  "materials": string[] | null, "safetyNote": string | null,
  "tags": string[], "category": string }`;
}

export class ClaudeDraftGenerator implements DraftGenerationPort {
  private readonly client: Anthropic;

  constructor(client: Anthropic = new Anthropic()) {
    this.client = client;
  }

  async generateDraft(request: DraftGenerationRequest): Promise<GeneratedDraftFields> {
    const response = await this.client.messages.parse({
      model: MODEL,
      max_tokens: 4096,
      messages: [{ role: "user", content: buildPrompt(request) }],
      output_config: {
        format: zodOutputFormat(GeneratedDraftSchema),
      },
    });

    if (!response.parsed_output) {
      throw new Error("Claude did not return a parseable draft.");
    }

    return response.parsed_output;
  }
}
