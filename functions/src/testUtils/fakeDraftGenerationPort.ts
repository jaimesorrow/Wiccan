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
      intro: `Inspired by ${request.tradition}.`,
      body: "A wholly original body of text, never lifted from the source.",
      reflection: this.reflectionOverride === undefined
        ? "What does this historical evidence help you notice?"
        : this.reflectionOverride,
      practice: null,
      materials: request.type === "potion" ? ["a bowl of water", "a white candle"] : null,
      safetyNote: request.type === "potion" ? "Keep away from open flame." : null,
      tags: [request.type],
      category: "MoonAndSeasonal",
    };
  }
}
