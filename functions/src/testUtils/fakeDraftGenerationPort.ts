import { DraftGenerationPort, DraftGenerationRequest, GeneratedDraftFields } from "../draftGenerationPort";

/** In-memory stand-in for Claude — returns a deterministic draft per call, no network/paid calls. */
export class FakeDraftGenerationPort implements DraftGenerationPort {
  calls: DraftGenerationRequest[] = [];
  /** When set, every call returns this title instead of an auto-incrementing one - for duplicate-title tests. */
  titleOverride: string | null = null;
  /** Allows validation tests to make the normally valid reflection malformed. */
  reflectionOverride: string | null | undefined;
  private counter = 0;

  async generateDraft(request: DraftGenerationRequest): Promise<GeneratedDraftFields> {
    this.calls.push(request);
    this.counter += 1;
    return {
      title: this.titleOverride ?? `Generated ${request.type} #${this.counter}`,
      summary: "A source-grounded practical lesson.",
      intro: `Inspired by ${request.tradition}.`,
      body: "1. Prepare the documented materials safely.\n2. Complete and close the documented method.",
      reflection: this.reflectionOverride === undefined
        ? "What does this historical evidence help you notice?"
        : this.reflectionOverride,
      practice: "Complete the numbered method once.",
      materials: request.type === "potion" ? ["a bowl of water", "a white candle"] : [],
      tools: request.type === "potion" ? ["a bowl of water", "a white candle"] : [],
      safetyNote: request.type === "potion" ? "Keep away from open flame." : "Stop if the practice causes distress.",
      tags: [request.type],
      category: "divination",
      primaryCategoryId: "divination",
      secondaryCategoryIds: [],
      lessonType: request.type === "potion" ? "potion" : "divination",
      requiresPotionDisclosure: request.type === "potion",
      durationMinutes: null,
      incantation: null,
      incantationOmissionReason: "The passage has no approved formula for inclusion.",
      timing: null,
      application: "A clearly labeled modern witchcraft application.",
      correspondences: [],
      adaptationNotes: [],
    };
  }
}
