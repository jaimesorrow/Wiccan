import {
  generationConceptSignature,
  normalizeGenerationText,
  screenGeneratedCandidate,
} from "../generationOriginality";
import { FakeContentAdminRepository } from "../testUtils/fakeContentAdminRepository";
import type { NewDraftContent } from "../contentAdminRepositoryPort";

function lesson(overrides: Record<string, unknown> = {}): Record<string, unknown> {
  return {
    title: "Threshold Grounding Cord",
    slug: "threshold-grounding-cord",
    summary: "Tie a three-knot cord to mark a calm return through the doorway.",
    introduction: "A modern threshold practice.",
    lesson: "1. Hold the cord at the doorway.\n2. Tie three knots while breathing slowly.\n3. Place the cord near the threshold.",
    practice: "Pause at the threshold for seven days.",
    reflection: "What changed when you paused?",
    safetyNote: "Keep loose cord away from children and pets.",
    primaryCategoryId: "cord-knot-work",
    secondaryCategoryIds: ["altars-sacred-space"],
    lessonType: "cord",
    tools: ["cotton cord"],
    correspondences: ["threshold", "three knots"],
    tags: ["grounding", "home"],
    type: "lesson",
    sourcePassageId: "passage-threshold",
    ...overrides,
  };
}

describe("generation originality screen", () => {
  it("normalizes tool synonyms before comparison", () => {
    expect(normalizeGenerationText("Thread, yarn, and string")).toBe("cord cord and cord");
  });

  it("extracts a structured practical concept signature", () => {
    expect(generationConceptSignature(lesson())).toMatchObject({
      primaryMethod: "cord knot work",
      primaryTool: "cotton cord",
      setting: "threshold doorway",
      ritualActions: expect.arrayContaining(["hold the cord at the doorway"]),
    });
  });

  it("includes structured tool and correspondence values in the signature", () => {
    const signature = generationConceptSignature(lesson({
      tools: [{ name: "Cotton cord", quantity: "one piece", required: true }],
      correspondences: { elements: ["Earth"], symbols: ["Threshold"] },
    }));
    expect(signature.primaryTool).toBe("cotton cord");
    expect(signature.supportingTools).toContain("one piece");
    expect(signature.symbolicStructure).toContain("earth");
    expect(signature.symbolicStructure).toContain("threshold");
  });

  it("rejects the same ritual when it is renamed and uses string synonyms", () => {
    const existing = lesson();
    const candidate = lesson({
      title: "Doorway Steadiness Thread",
      slug: "doorway-steadiness-thread",
      tools: ["cotton string"],
      lesson: "1. Hold the string at the doorway.\n2. Tie three knots while breathing slowly.\n3. Place the string near the threshold.",
    });
    const report = screenGeneratedCandidate(candidate, [{ id: "existing", fields: existing }]);
    expect(report.accepted).toBe(false);
    expect(report.rejectionReasons.some((reason) => reason.includes("instruction-similarity"))).toBe(true);
    expect(report.closestMatch?.comparedId).toBe("existing");
  });

  it("allows distinct practices that happen to share one tool", () => {
    const candidate = lesson({
      title: "Measured Cord Circle",
      slug: "measured-cord-circle",
      sourcePassageId: "passage-measured-cord",
      summary: "Use cord as a temporary measuring line for a small altar cloth.",
      introduction: "A practical setup exercise.",
      lesson: "1. Lay the cloth flat on a table.\n2. Measure its edge with loose cord.\n3. Coil and store the cord after recording the measurement.",
      practice: "Use the measurement to choose a safely sized altar surface.",
      reflection: "How did measuring change the way you arranged the space?",
      correspondences: [],
      tags: ["altar", "measurement"],
    });
    expect(screenGeneratedCandidate(candidate, [{ id: "existing", fields: lesson() }]).accepted).toBe(true);
  });

  it("rejects a count-only variation of the same ritual method", () => {
    const candidate = lesson({
      title: "Sevenfold Doorway Cord",
      slug: "sevenfold-doorway-cord",
      summary: "Tie a seven-knot cord to mark a calm return through the doorway.",
      lesson: "1. Hold the cord at the doorway.\n2. Tie seven knots while breathing slowly.\n3. Place the seven knots near the threshold.",
      practice: "Pause at the threshold for three days.",
      correspondences: ["threshold", "seven knots"],
    });

    const report = screenGeneratedCandidate(candidate, [{ id: "existing", fields: lesson() }]);

    expect(report.accepted).toBe(false);
    expect(report.rejectionReasons.some((reason) => reason.includes("ritual-action-similarity"))).toBe(true);
  });

  it("uses atomic fake reservations to reject a concurrent duplicate", async () => {
    const repo = new FakeContentAdminRepository();
    const report = screenGeneratedCandidate(lesson(), []);
    const draft = lesson() as unknown as NewDraftContent;
    const results = await Promise.allSettled([
      repo.createDraftWithOriginalityReservations(draft, report.reservationKeys),
      repo.createDraftWithOriginalityReservations(draft, report.reservationKeys),
    ]);

    expect(results.filter((result) => result.status === "fulfilled")).toHaveLength(1);
    expect(results.filter((result) => result.status === "rejected")).toHaveLength(1);
    expect(repo.docs.size).toBe(1);
  });

  it("rejects a second item that reuses the same sourcePassageId and type", () => {
    const report = screenGeneratedCandidate(
      lesson({
        title: "A Completely Different Doorway Title",
        slug: "a-completely-different-doorway-title",
        summary: "Light a lamp and write a short note about the evening light.",
        introduction: "A lamp-lighting observation.",
        lesson: "1. Trim a lamp wick.\n2. Light it at dusk and snuff it at night.\n3. Write one sentence about the light.",
        practice: "Keep the written sentence with the lamp.",
        reflection: "What did the lamplight change?",
        correspondences: ["lamp"],
        tags: ["light"],
      }),
      [{ id: "existing", fields: lesson() }],
    );
    expect(report.accepted).toBe(false);
    expect(report.rejectionReasons.some((reason) => reason.startsWith("source-passage-id:"))).toBe(true);
    expect(report.reservationKeys.some((key) => key.startsWith("passage-type:"))).toBe(true);
  });

  it("allows the same passage when the content type differs and the practice is distinct", () => {
    const candidate = lesson({
      type: "spell",
      title: "Measured Cord Circle",
      slug: "measured-cord-circle",
      summary: "Use cord as a temporary measuring line for a small altar cloth.",
      introduction: "A practical setup exercise.",
      lesson: "1. Lay the cloth flat on a table.\n2. Measure its edge with loose cord.\n3. Coil and store the cord after recording the measurement.",
      practice: "Use the measurement to choose a safely sized altar surface.",
      reflection: "How did measuring change the way you arranged the space?",
      correspondences: [],
      tags: ["altar", "measurement"],
    });
    expect(screenGeneratedCandidate(candidate, [{ id: "existing", fields: lesson() }]).accepted).toBe(true);
  });

  it("rejects near-duplicate authored text by trigram Jaccard even with a new title and passage", () => {
    const existing = lesson();
    const candidate = lesson({
      title: "Calm Return Through The Door",
      slug: "calm-return-through-the-door",
      sourcePassageId: "passage-other",
      summary: existing.summary,
      introduction: existing.introduction,
      lesson: existing.lesson,
      practice: existing.practice,
      reflection: existing.reflection,
    });
    const report = screenGeneratedCandidate(candidate, [{ id: "existing", fields: existing }]);
    expect(report.accepted).toBe(false);
    expect(report.rejectionReasons.some((reason) =>
      reason.includes("content-shingle-similarity") || reason.includes("instruction-similarity") || reason.includes("summary-similarity")
    )).toBe(true);
  });
});
