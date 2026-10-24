import {
  conceptFingerprint,
  curriculumConcept,
  findLexicalDuplicateConcerns,
  longestCommonTokenRunRatio,
  ngramJaccard,
  screenSemanticSimilarity,
} from "../contentSimilarity";

const lesson = (id: string, title: string, body: string) => curriculumConcept(id, {
  title,
  sourcePassageId: "folio-1",
  lessonType: "cord",
  summary: "Tie a cord to remember a boundary.",
  tools: ["cord"],
  body,
  practice: "Tie and release one knot.",
});
describe("shared curriculum similarity", () => {
  it("uses the practical concept rather than the title for fingerprints", () => {
    expect(conceptFingerprint(lesson("a", "First title", "1. Tie one knot. 2. Release it.")))
      .toBe(conceptFingerprint(lesson("b", "Different title", "1. Tie one knot. 2. Release it.")));
  });

  it("flags renamed procedures with the same passage and technique", () => {
    const concerns = findLexicalDuplicateConcerns([
      lesson("a", "First title", "1. Tie one knot. 2. Release it safely."),
      lesson("b", "Second title", "1. Tie one knot. 2. Release it safely."),
    ]);
    expect(concerns[0].reasons).toContain("same-passage-and-technique");
  });

  it("calculates bounded lexical overlap", () => {
    expect(ngramJaccard("one two three four", "one two three five")).toBeGreaterThan(0);
    expect(longestCommonTokenRunRatio("one two three four", "zero two three five")).toBe(0.5);
  });

  it("routes configurable semantic scores to review or rejection", async () => {
    const [left, right] = [lesson("a", "A", "one"), lesson("b", "B", "two")];
    const results = await screenSemanticSimilarity([[left, right]], { name: "fake", similarity: async () => 0.93 });
    expect(results[0].disposition).toBe("reject");
  });
});
