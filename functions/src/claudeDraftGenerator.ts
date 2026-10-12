import Anthropic from "@anthropic-ai/sdk";
import { z } from "zod";
import { zodOutputFormat } from "@anthropic-ai/sdk/helpers/zod";
import { DraftGenerationPort, DraftGenerationRequest, GeneratedDraftFields } from "./draftGenerationPort";
import { CONTENT_CATEGORIES } from "./types";

const MODEL = "claude-opus-5";

const GeneratedDraftSchema = z.object({
  title: z.string().trim().min(1),
  intro: z.string().trim().min(1),
  body: z.string().trim().min(1),
  reflection: z.string().trim().min(1).regex(/\?$/, "reflection must end with a question mark"),
  practice: z.string().trim().min(1).nullable(),
  materials: z.array(z.string().trim().min(1)).min(1).nullable(),
  safetyNote: z.string().trim().min(1).nullable(),
  tags: z.array(z.string().trim().min(1)).min(1),
  category: z.enum(CONTENT_CATEGORIES),
});

/**
 * Builds Wiccan's editorial prompt.
 *
 * Every draft is grounded in an editor-approved ancient grimoire or magical
 * manuscript. Factual claims about antiquity must stay within the curated
 * groundedFacts supplied by an editor. Modern adaptations must never pretend
 * to reproduce an ancient rite or quotation.
 *
 * The voice and safety rules are production requirements, not suggestions.
 * See docs/CONTENT_VOICE_AND_LESSON_STANDARD.md.
 */
function buildPrompt(request: DraftGenerationRequest): string {
  const groundedFacts = request.groundedFacts.map((fact, index) => `${index + 1}. ${fact}`).join("\n");

  return `You are drafting one original ${request.type} for Wiccan, a modern witchcraft app.

VOICE
Write in a grounded, mature, historically careful, practical, reflective,
subtly grimoire-inspired voice. The writing must feel serious and credible,
not theatrical or cheesy. Use clear modern English. Do not use fake archaic
English, sensational occult language, horror imagery, or fantasy narration.

SOURCE
This is an editor-approved ancient grimoire or ancient magical manuscript.
Title: ${request.sourceTitle}
Tradition: ${request.tradition}
Period: ${request.period}
Editorial reference: ${request.sourceReference}
Background summary: ${request.summary}

CURATED HISTORICAL FACTS
${groundedFacts}

HISTORICAL INTEGRITY
The draft's subject and historical basis must come from the approved ancient
source above. Treat the CURATED HISTORICAL FACTS as the boundary for every
claim about antiquity. Do not add material from modern occult books, general
model knowledge, or an unlisted historical source. You may explain the facts'
significance in your own words, but you must not invent quotations,
translations, dates, ritual steps, deities, objects, lineages, or historical
claims that are not supported by those facts. Never fabricate a primary-source
quotation.

If type=lesson, the lesson must teach real historical information grounded
in those facts.

Do not describe ancient Egyptian, Mesopotamian, Greek, Roman, or other
pre-modern traditions as "ancient Wicca" or imply that modern Wicca is an
unchanged survival of them. Present the ancient material on its own terms,
then clearly label any modern reflection or optional practice as a modern
adaptation inspired by the historical material rather than an authentic
reconstruction.

If the evidence is partial, contested, fragmentary, or culturally specific,
state that limitation rather than filling the gap. Distinguish historical
practice, scholarly interpretation, symbolism, and modern personal reflection.

If type=spell or type=potion, create a clearly labeled modern, safe adaptation
whose theme is traceable to the CURATED HISTORICAL FACTS. Do not claim that
the generated instructions are an authentic ancient rite, translation, or
reconstruction. Do not quote or closely paraphrase a source.

CATEGORY
Choose exactly one category from:
${CONTENT_CATEGORIES.join(", ")}

LESSON FORMAT
If type=lesson, structure the content so it reads naturally as:
1. Introduction — concise factual/contextual opening in intro.
2. Lesson — substantial explanation in body.
3. Reflection — put one thoughtful question in the separate reflection field.
4. Practice — optional, in the separate practice field, only when a safe simple modern reflection genuinely adds value.
5. Safety Note — use safetyNote whenever risk warrants it.

For a lesson, prioritize historical understanding over spell instructions.
A lesson may describe an attested ritual historically without instructing the
reader to reproduce hazardous, coercive, medical, funerary, or culturally
restricted practices.

Keep reflection and practice separate from body. Reflection must be specific
to the lesson, not generic filler; set practice to null when it adds no value.
Do not claim the moon, herbs, symbols, salt, ritual, or other practices
objectively cause supernatural outcomes. You may explain that practitioners
used or use them symbolically, devotionally, ritually, historically, or as a
framework for attention and reflection.

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
- turn historical descriptions of harmful, coercive, or dangerous rites into instructions

If the piece involves open flame, smoke/incense, allergens, pets, children,
skin contact, ingestion, plants, or another meaningful risk, include a clear
safetyNote. Set safetyNote to null only when no meaningful safety note is needed.

OUTPUT
Return strict JSON matching:
{ "title": string, "intro": string, "body": string,
  "reflection": string, "practice": string | null,
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
