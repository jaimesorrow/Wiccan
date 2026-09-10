import { DraftGenerationPort, DraftGenerationRequest, GeneratedDraftFields } from "../draftGenerationPort";

/** In-memory stand-in for Claude — returns a deterministic draft per call, no network/paid calls. */
export class FakeDraftGenerationPort implements DraftGenerationPort {
  calls: DraftGenerationRequest[] = [];
  private counter = 0;

  async generateDraft(request: DraftGenerationRequest): Promise<GeneratedDraftFields> {
    this.calls.push(request);
    this.counter += 1;
    return {
      title: `Generated ${request.type} #${this.counter}`,
      intro: `Inspired by ${request.tradition}.`,
      body: "A wholly original body of text, never lifted from the source.",
      materials: request.type === "potion" ? ["a bowl of water", "a white candle"] : null,
      safetyNote: request.type === "potion" ? "Keep away from open flame." : null,
      tags: [request.type],
    };
  }
}
