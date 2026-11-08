import { checkDuplicates, conceptSignature, DEFAULT_THRESHOLDS, indexLesson, normalizedTitle, ritualActions } from "../lessons/duplicates";
import { CanonicalLesson } from "../lessons/canonicalLesson";
import { lunarBraidCalendar, thresholdGrounding } from "../testUtils/lessonFixtures";

const base = thresholdGrounding();
const inventory = [indexLesson(base, "repo:fixture")];
const check = (candidate: CanonicalLesson, thresholds = DEFAULT_THRESHOLDS, inv = inventory) =>
  checkDuplicates(indexLesson(candidate, "run"), inv, thresholds);
const fresh = (overrides: Partial<CanonicalLesson>): CanonicalLesson =>
  ({ ...thresholdGrounding(), id: "candidate", slug: "candidate-slug", title: "Candidate Title Placeholder", ...overrides });

describe("lesson duplicate detection", () => {
  it("rejects an exact document ID", () => {
    const report = check({ ...base });
    expect(report.status).toBe("rejected-duplicate");
    expect(report.rejectionReasons.join(" ")).toMatch(/same document ID/);
  });

  it("rejects a duplicate normalized title and a duplicate slug", () => {
    expect(check(fresh({ title: "The Threshold Grounding for a Calm Home!" })).rejectionReasons.join(" ")).toMatch(/normalized title/);
    expect(check(fresh({ slug: base.slug })).rejectionReasons.join(" ")).toMatch(/same slug/);
  });

  it("normalizes synonyms, plurals and decorative words in titles", () => {
    expect(normalizedTitle("A Simple Taper Charm for the Doorways")).toBe(normalizedTitle("Candle Talisman Threshold"));
  });

  it("rejects the same ritual with changed colours, herbs and incantation", () => {
    const recoloured = fresh({
      title: "Doorway Knots for a Peaceful House",
      lesson: base.lesson.replace("natural-fiber cord", "red cord sprinkled with dried rosemary").replace(/three slow breaths/, "seven slow breaths"),
      incantation: "Here at my door I arrive; what stays outside stays outside; peace lives here.",
      correspondences: { ...base.correspondences, colors: ["Red"], herbs: ["Rosemary"] },
    });
    const report = check(recoloured);
    expect(report.status).toBe("rejected-duplicate");
    expect(report.instructionSimilarity).toBeGreaterThanOrEqual(DEFAULT_THRESHOLDS.instructions);
  });

  it("rejects reordered but equivalent instructions", () => {
    const steps = base.lesson.split("\n\n");
    const reordered = fresh({ title: "Knotted Cord Welcome Practice", lesson: [steps[1], steps[0], steps[2], steps[4], steps[3], steps[5]].join("\n\n") });
    const report = check(reordered);
    expect(report.status).toBe("rejected-duplicate");
    expect(report.ritualActionSimilarity).toBeGreaterThanOrEqual(DEFAULT_THRESHOLDS.ritualActions);
  });

  it("rejects the same concept filed under a different category", () => {
    const report = check(fresh({ title: "Sacred Space Entry Knots", primaryCategoryId: "altars-sacred-space", secondaryCategoryIds: ["cord-knot-work"] }));
    expect(report.status).toBe("rejected-duplicate");
  });

  it("rejects a paraphrased summary of the same practice", () => {
    const report = check(fresh({
      title: "Entering Home With Three Knots",
      summary: "Make a simple cord with three knots to mark moving from the outside world's noise into your home's calm.",
    }));
    expect(report.status).toBe("rejected-duplicate");
  });

  it("accepts a distinct practice that shares the cord tool, and explains why", () => {
    const report = check(lunarBraidCalendar());
    expect(report.rejectionReasons).toEqual([]);
    expect(report.status).toBe("accepted-draft");
    expect(report.closestMatchId).toBe(base.id);
    expect(report.distinctiveFeatures.length).toBeGreaterThanOrEqual(DEFAULT_THRESHOLDS.minimumNoveltyAreas);
  });

  it("rejects a duplicate generated earlier in the same batch", () => {
    const first = lunarBraidCalendar();
    const batch = [...inventory, indexLesson(first, "run")];
    const second = { ...first, id: "lunar-braid-again", slug: "lunar-braid-again", title: "Nightly Braid Moon Record" };
    expect(check(second, DEFAULT_THRESHOLDS, batch).status).toBe("rejected-duplicate");
  });

  it("uses configurable thresholds without changing the defaults", () => {
    const paraphrase = fresh({ title: "Entering Home With Three Knots", summary: "A cord with knots for coming home calmly." });
    const lenient = { ...DEFAULT_THRESHOLDS, title: 2, summary: 2, instructions: 2, combined: 2, ritualActions: 2, concept: 2, minimumNoveltyAreas: 0 };
    expect(check(paraphrase).status).toBe("rejected-duplicate");
    // Exact fingerprints still apply even with lenient similarity thresholds.
    expect(check({ ...paraphrase, slug: base.slug }, lenient).status).toBe("rejected-duplicate");
    expect(DEFAULT_THRESHOLDS.instructions).toBe(0.78);
  });

  it("produces stable, deterministic results", () => {
    const a = check(lunarBraidCalendar());
    const b = check(lunarBraidCalendar());
    expect(a).toEqual(b);
  });

  it("derives a concept signature from the content, not the tags", () => {
    const signature = conceptSignature(base);
    expect(signature.primaryMethod).toBe("cord-knot-work");
    expect(signature.primaryTool).toBe("cord");
    expect(signature.setting).toBe("home entrance");
    expect(ritualActions(base.lesson)).toEqual(expect.arrayContaining([expect.stringMatching(/^knot /)]));
  });
});
