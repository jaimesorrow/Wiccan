import {
  findPracticalLessonViolations,
  findPracticalSafetyViolations,
  isPracticalCategory,
  PRACTICAL_CATEGORIES,
  practicalContentHash,
} from "../practicalLesson";
import type { SourcePassage } from "../sourcePassages";

const PASSAGE: SourcePassage = {
  id: "papyrus-col-v",
  sourceId: "london-leiden-demotic-magical-papyrus",
  textTitle: "The Demotic Magical Papyrus of London and Leiden",
  manuscriptId: "P. BM EA 10070 / P. Leiden I 383",
  tradition: "Roman-period Egyptian magical practice",
  manuscriptDate: "Early third century CE",
  compositionDate: "Roman period",
  edition: "F. Ll. Griffith and Herbert Thompson, vol. I",
  editionDate: "1904",
  translator: "F. Ll. Griffith and Herbert Thompson",
  locator: "Recto col. V, lines 3–24",
  url: "https://example.org/inspected-edition",
  inspectedPassage: "Editor-inspected test transcription of a lamp inquiry.",
  attestedInstructions: ["Prepare a lamp.", "Lie down in silence for a dream response."],
  uncertainties: [],
  formula: null,
  formulaOmissionReason: "The formula is omitted from this test lesson.",
  timing: "At night before sleep.",
  dating: {
    era: "ancient",
    status: "verified",
    evidenceUrl: "https://example.org/catalogue",
    notes: "Catalogue dates the manuscript to the early third century CE.",
  },
  rights: {
    status: "verified",
    basis: "The reviewed edition is in the public domain for the listed jurisdiction.",
    reference: "https://example.org/rights",
    jurisdictions: ["US"],
  },
  scope: {
    status: "approved",
    rationale: "The attested divination sequence supports a clearly labeled modern witchcraft adaptation.",
    evidenceUrl: "https://example.org/scope",
  },
  review: {
    passageInspected: true,
    sourceFidelityApproved: true,
    safetyApproved: true,
    reviewedBy: "source editor",
    reviewedAt: "2026-10-19T00:00:00.000Z",
  },
};

function reviewedLesson(): Record<string, unknown> {
  const content: Record<string, unknown> = {
    title: "A Lamp and Dream Inquiry",
    type: "lesson",
    tier: "free",
    tradition: PASSAGE.tradition,
    summary: "A source-grounded dream reflection.",
    intro: "This is a modern adaptation of a Roman-period Egyptian lamp inquiry.",
    body: "1. Prepare a safe flameless light.\n2. Record the dream and close the inquiry.",
    reflection: "What did you notice when you treated the dream as material for reflection?",
    practice: "Complete the numbered method once.",
    materials: ["A flameless light", "A notebook"],
    tools: ["A flameless light", "A notebook"],
    safetyNote: "Use a flameless light near sleeping areas.",
    tags: ["dream", "divination"],
    category: "divination",
    primaryCategoryId: "divination",
    secondaryCategoryIds: [],
    lessonType: "divination",
    requiresPotionDisclosure: false,
    durationMinutes: null,
    incantation: null,
    incantationOmissionReason: "The source formula is omitted rather than reconstructed.",
    timing: "The source places the inquiry at night before sleep.",
    application: "A clearly labeled modern witchcraft dream-reflection practice.",
    correspondences: [],
    adaptationNotes: ["An electric light replaces the source lamp for safety."],
    sourceId: PASSAGE.sourceId,
    sourcePassageId: PASSAGE.id,
    sourceCitation: PASSAGE.locator,
    sourceUrl: PASSAGE.url,
    citations: [{
      textTitle: PASSAGE.textTitle,
      manuscriptId: PASSAGE.manuscriptId,
      tradition: PASSAGE.tradition,
      manuscriptDate: PASSAGE.manuscriptDate,
      compositionDate: PASSAGE.compositionDate,
      edition: PASSAGE.edition,
      editionDate: PASSAGE.editionDate,
      translator: PASSAGE.translator,
      locator: PASSAGE.locator,
      url: PASSAGE.url,
    }],
    rightsReviewRequired: false,
    status: "draft",
  };
  content.practicalReview = {
    reviewedBy: "lesson editor",
    reviewedAt: "2026-10-19T01:00:00.000Z",
    contentHash: practicalContentHash(content),
    sourceFidelityApproved: true,
    scopeApproved: true,
    safetyApproved: true,
    rightsApproved: true,
    duplicateReviewed: true,
  };
  return content;
}

describe("practical lesson gate", () => {
  it("recognizes every proposed practical category and no legacy intention category", () => {
    expect(PRACTICAL_CATEGORIES).toHaveLength(10);
    for (const category of PRACTICAL_CATEGORIES) expect(isPracticalCategory(category)).toBe(true);
    expect(isPracticalCategory("LoveAndRelationships")).toBe(false);
  });

  it("accepts a passage-grounded lesson with a current complete review", () => {
    expect(findPracticalLessonViolations(reviewedLesson(), [PASSAGE])).toEqual([]);
  });

  it("invalidates approval when authored content changes", () => {
    const content = reviewedLesson();
    content.body = "1. A changed method.\n2. Close it.";
    expect(findPracticalLessonViolations(content, [PASSAGE])).toContain(
      "current content needs a dated, hash-bound fidelity, scope, rights, safety and duplicate review",
    );
  });

  it("blocks unlabeled modern wording when the inspected passage has no approved formula", () => {
    const content = reviewedLesson();
    content.incantation = "An invented line";
    expect(findPracticalLessonViolations(content, [PASSAGE])).toContain(
      "a modern incantation absent from the source must be labeled as original modern wording",
    );
  });

  it("blocks a broad source claim without an exact registered passage", () => {
    const content = reviewedLesson();
    content.sourcePassageId = "unknown-passage";
    expect(findPracticalLessonViolations(content, [PASSAGE])).toContain(
      "verified source passage is missing from the editor-owned register",
    );
  });

  it.each([
    ["1. Drink this ritual oil.\n2. Finish.", "generated practical lessons may not instruct ingestion"],
    ["1. Make them love you without their consent.\n2. Finish.", "coercive targeting"],
    ["1. This working guarantees wealth.\n2. Finish.", "guaranteed outcome"],
    ["1. Burn the candle.\n2. Finish.", "flame-related lesson"],
  ])("flags unsafe generated instructions: %s", (body, expected) => {
    const content = { ...reviewedLesson(), body, lesson: body, safetyNote: "Be careful." };
    expect(findPracticalSafetyViolations(content).join(" ")).toContain(expected);
  });
});
