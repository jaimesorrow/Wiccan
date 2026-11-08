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
    const methods = [
      {
        summary: "Observe a marked token and record how its shape directs attention.",
        body: "1. Place the documented token on a stable surface.\n2. Trace its outline without touching it and record the observation.",
        practice: "Record one visual detail from the marked token.",
      },
      {
        summary: "Arrange the documented pieces to study sequence and deliberate closure.",
        body: "1. Set the documented pieces in their recorded order.\n2. Reverse the arrangement and return every piece to storage.",
        practice: "Repeat the arrangement once and note where the sequence required care.",
      },
      {
        summary: "Use a measured pause to examine the documented boundary practice.",
        body: "1. Mark the documented boundary without crossing it.\n2. Pause, remove the marker, and leave the area clear.",
        practice: "Describe how the boundary changed your attention to the space.",
      },
      {
        summary: "Compare two documented symbols through a quiet written observation.",
        body: "1. Place the two documented symbols side by side.\n2. Write one difference, then cover and store both symbols.",
        practice: "Review the written difference without predicting an outcome.",
      },
      {
        summary: "Practice a documented opening and closing gesture with careful pacing.",
        body: "1. Perform the documented opening gesture once.\n2. Complete the distinct closing gesture and return to a neutral posture.",
        practice: "Note which part of the opening and closing sequence felt clearest.",
      },
    ];
    const method = methods[(this.counter - 1) % methods.length];
    return {
      title: this.titleOverride ?? `Generated ${request.type} #${this.counter}`,
      summary: method.summary,
      intro: `Inspired by ${request.tradition}.`,
      body: method.body,
      reflection: this.reflectionOverride === undefined
        ? "What does this historical evidence help you notice?"
        : this.reflectionOverride,
      practice: method.practice,
      materials: request.type === "potion" ? ["a bowl of water", "a white candle"] : [],
      tools: request.type === "potion" ? ["a bowl of water", "a white candle"] : [],
      safetyNote: request.type === "potion"
        ? "Use a flameless light, keep the preparation external, and patch test for skin irritation."
        : "Stop if the practice causes distress.",
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
