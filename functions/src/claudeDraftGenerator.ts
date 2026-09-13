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
 * Builds Wiccan's editorial prompt. The source is thematic inspiration
 * only — never quoted, closely paraphrased, or structurally mirrored.
 * `summary` is a vetted source summary, never a verbatim excerpt.
 *
 * The voice and safety rules are production requirements, not suggestions.
 * See docs/CONTENT_VOICE_AND_LESSON_STANDARD.md.
 */
function buildPrompt(request: DraftGenerationRequest): string {
  return `You are drafting one original ${request.type} for Wiccan, a modern witchcraft app.

VOICE
Write in a grounded, mature, historically careful, practical, reflective,
subtly grimoire-inspired voice. The writing must feel serious and credible,
not theatrical or cheesy. Use clear modern English. Do not use fake archaic
English, sensational occult language, horror imagery, or fantasy narration.

SOURCE USE
Use the following historical tradition ONLY as thematic inspiration. Do not
quote it, closely paraphrase it, mirror its structure, or imply uncertain
historical claims are established fact. Clearly distinguish historical or
customary practice, modern practice, symbolism, and personal reflection.

Tradition: ${request.tradition}
Background (for inspiration only, not to be echoed): ${request.summary}

CATEGORY
Choose exactly one category from:
${CONTENT_CATEGORIES.join(", ")}

LESSON FORMAT
If type=lesson, structure the content so it reads naturally as:
1. Introduction — concise factual/contextual opening in intro.
2. Lesson — substantial explanation in body.
3. Reflection — end body with a clearly labeled "Reflection" section and one thoughtful question.
4. Practice — optional, clearly labeled, only when a safe simple action genuinely adds value.
5. Safety Note — use safetyNote whenever risk warrants it.

Reflection must be specific to the lesson, not generic filler.
Do not claim the moon, herbs, symbols, salt, ritual, or other practices
objectively cause supernatural outcomes. You may explain that practitioners
use them symbolically, devotionally, ritually, historically, or as a framework
for attention and reflection.

SPELLS AND POTIONS
For spells and potions, keep the same grounded philosophy. Distinguish
symbolism/tradition from established physical effects. Give clear materials
and steps when appropriate. Never promise guaranteed love, wealth, healing,
protection, divination accuracy, or another supernatural result. Treat
"potion" cautiously: prefer non-ingestible ritual preparations unless every
ingredient and intended use is plainly food-safe.

SAFETY — the draft must NOT:
- diagnose, treat, or claim to cure any medical or mental health condition
- present ritual as a replacement for professional medical, legal, financial, or emergency care
- instruct ingestion of anything unsafe or any unidentified wild plant
- call for toxic, poisonous, or otherwise hazardous materials
- guarantee a specific supernatural outcome
- present folklore or symbolism as scientific fact

If the piece involves open flame, smoke/incense, allergens, pets, children,
skin contact, ingestion, plants, or another meaningful risk, include a clear
safetyNote. Set safetyNote to null only when no meaningful safety note is needed.

OUTPUT
Return strict JSON matching:
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
