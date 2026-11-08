import { CanonicalLesson, toCanonicalLesson } from "../lessons/canonicalLesson";
import { inferCategoryFromText, LESSON_CATEGORY_IDS } from "../lessons/categories";
import { validateLesson } from "../lessons/validateLesson";
import { thresholdGrounding } from "../testUtils/lessonFixtures";

const messages = (lesson: CanonicalLesson) => validateLesson(lesson).map((issue) => `${issue.code}:${issue.field}:${issue.message}`);

describe("lesson validation", () => {
  it("accepts the approved reference lesson", () => {
    expect(validateLesson(thresholdGrounding())).toEqual([]);
  });

  it("defines exactly the ten practical categories", () => {
    expect(LESSON_CATEGORY_IDS).toEqual([
      "candle-spells", "herbal-rituals", "cord-knot-work", "lunar-practices", "charms-talismans",
      "crystals-stones", "divination", "altars-sacred-space", "elemental-practices", "potions-infusions",
    ]);
  });

  it("rejects missing required fields and empty instructions", () => {
    const issues = messages({ ...thresholdGrounding(), summary: "", lesson: "", safetyNote: "null" });
    expect(issues).toEqual(expect.arrayContaining([
      expect.stringMatching(/^schema:summary/), expect.stringMatching(/^schema:lesson/), expect.stringMatching(/^schema:safetyNote/),
    ]));
  });

  it("rejects unknown categories and unsupported status", () => {
    const issues = messages({ ...thresholdGrounding(), primaryCategoryId: "love" as never, contentStatus: "live" as never });
    expect(issues).toEqual(expect.arrayContaining([expect.stringMatching(/primaryCategoryId/), expect.stringMatching(/contentStatus/)]));
  });

  it("requires the potion disclosure and a ritual-only label for preparations", () => {
    const issues = messages({ ...thresholdGrounding(), lessonType: "bath", requiresPotionDisclosure: false });
    expect(issues).toEqual(expect.arrayContaining([expect.stringMatching(/requiresPotionDisclosure/), expect.stringMatching(/Ritual use only/)]));
  });

  it("rejects essential-oil ingestion, guaranteed outcomes, coercion and medical claims", () => {
    const lesson = thresholdGrounding();
    const bad = (sentence: string) => messages({ ...lesson, lesson: `${lesson.lesson}\n\n${sentence}` });
    expect(bad("Add three drops of essential oil to your tea and drink it.")).toEqual(expect.arrayContaining([expect.stringMatching(/essential oils/)]));
    expect(bad("This spell is guaranteed to bring a new job within a week.")).toEqual(expect.arrayContaining([expect.stringMatching(/guaranteed/)]));
    expect(bad("Use it to make him love you again.")).toEqual(expect.arrayContaining([expect.stringMatching(/coercive/)]));
    expect(bad("This ritual cures anxiety.")).toEqual(expect.arrayContaining([expect.stringMatching(/medical/)]));
    expect(bad("Perform the working without their consent.")).toEqual(expect.arrayContaining([expect.stringMatching(/coercive/)]));
    expect(bad("This spell is guaranteed to bring a new job with no effort.")).toEqual(expect.arrayContaining([expect.stringMatching(/guaranteed/)]));
  });

  it("allows negated outcome language", () => {
    expect(messages({ ...thresholdGrounding(), safetyNote: `${thresholdGrounding().safetyNote} It does not guarantee results.` })).toEqual([]);
  });

  it("requires safety guidance for fire, skin contact and allergies when the practice involves them", () => {
    const lesson = thresholdGrounding();
    const issues = messages({
      ...lesson,
      lesson: `${lesson.lesson}\n\nLight a candle and anoint your wrist with a lavender oil blend.`,
      safetyNote: "Keep cords away from children and pets.",
    });
    expect(issues).toEqual(expect.arrayContaining([
      expect.stringMatching(/address fire/), expect.stringMatching(/address skin contact/), expect.stringMatching(/address allergies/),
    ]));
  });

  it("detects plural hazard materials", () => {
    const lesson = thresholdGrounding();
    const issues = messages({
      ...lesson,
      lesson: `${lesson.lesson}\n\nLight three candles beside dried herbs.`,
      safetyNote: "Keep cords away from children and pets.",
    });
    expect(issues).toEqual(expect.arrayContaining([expect.stringMatching(/address fire/), expect.stringMatching(/address allergies/)]));
  });

  it("requires consent wording when another person could be affected", () => {
    const lesson = thresholdGrounding();
    expect(messages({ ...lesson, practice: `${lesson.practice} You may make a second cord for a friend.` }))
      .toEqual(expect.arrayContaining([expect.stringMatching(/consent/)]));
    expect(messages({
      ...lesson,
      practice: `${lesson.practice} You may make a second cord for a friend.`,
      safetyNote: `${lesson.safetyNote} Consent is not required.`,
    })).toEqual(expect.arrayContaining([expect.stringMatching(/consent/)]));
  });

  it("requires affirmative food-safety review for a consumable preparation", () => {
    const lesson = thresholdGrounding();
    const potion = { ...lesson, lessonType: "potion" as const, requiresPotionDisclosure: true, primaryCategoryId: "potions-infusions" as const };
    expect(messages({ ...potion, safetyNote: `${lesson.safetyNote} This preparation is not food-safe.` }))
      .toEqual(expect.arrayContaining([expect.stringMatching(/Ritual use only/)]));
    expect(messages({ ...potion, safetyNote: `${lesson.safetyNote} A separate spoon is food-safe.` }))
      .toEqual(expect.arrayContaining([expect.stringMatching(/Ritual use only/)]));
  });

  it("reports malformed runtime shapes instead of throwing", () => {
    const malformed = {
      ...thresholdGrounding(),
      lesson: null,
      tools: null,
      tags: null,
      secondaryCategoryIds: null,
      citations: [null],
    } as unknown as CanonicalLesson;
    expect(() => validateLesson(malformed)).not.toThrow();
    expect(messages(malformed)).toEqual(expect.arrayContaining([
      expect.stringMatching(/schema:lesson/), expect.stringMatching(/schema:tools/), expect.stringMatching(/schema:citations/),
    ]));
  });

  it("rejects citations that do not say which claim they support, and invented lineage", () => {
    const issues = messages({
      ...thresholdGrounding(),
      introduction: "As the ancient Wiccans taught, thresholds are sacred.",
      citations: [{ title: "Some Book", author: "A. Author", accessedAt: "2026-11-08", usageNote: "" }],
    });
    expect(issues).toEqual(expect.arrayContaining([expect.stringMatching(/usageNote/), expect.stringMatching(/ancient Wiccan/)]));
  });
});

describe("legacy compatibility mapping", () => {
  it("keeps the document ID and maps legacy fields without inventing content", () => {
    const { lesson, issues } = toCanonicalLesson("egypt-heka-001", {
      title: "Charging Water by Contact",
      intro: "This lesson examines the cippi of Horus.",
      body: "Write the recorded phrase in a journal.",
      materials: ["None"],
      category: "HealingAndWellness",
      type: "lesson",
      status: "draft",
      sourceCitation: "Brooklyn Museum 57.21.1",
      tags: ["egypt"],
    });
    expect(lesson.id).toBe("egypt-heka-001");
    expect(lesson.introduction).toBe("This lesson examines the cippi of Horus.");
    expect(lesson.lesson).toBe("Write the recorded phrase in a journal.");
    expect(lesson.tools).toEqual([]);
    expect(lesson.tags).toEqual(expect.arrayContaining(["egypt", "wellbeing"]));
    expect(lesson.contentStatus).toBe("draft");
    expect(issues).toEqual(expect.arrayContaining([expect.stringMatching(/no summary/), expect.stringMatching(/usageNote/)]));
    // An unmapped legacy record fails validation instead of reaching delivery.
    expect(validateLesson(lesson).length).toBeGreaterThan(0);
  });

  it("maps earlier practical category IDs and infers methods from intention categories", () => {
    expect(toCanonicalLesson("a", { title: "x", category: "candle-magic" }).lesson.primaryCategoryId).toBe("candle-spells");
    expect(toCanonicalLesson("b", { title: "Knot release", body: "Tie a cord.", category: "BanishingAndRelease" }).lesson.primaryCategoryId).toBe("cord-knot-work");
    expect(inferCategoryFromText("Light the taper candle")).toBe("candle-spells");
  });

  it("preserves citation textTitle and leaves a missing tradition empty", () => {
    const mapped = toCanonicalLesson("citation", {
      title: "Citation mapping",
      category: "divination",
      citations: [{ textTitle: "Primary text", institution: "Archive", accessedAt: "2026-11-08", usageNote: "Supports the date." }],
    }).lesson;
    expect(mapped.citations[0].title).toBe("Primary text");
    expect(mapped.tradition).toBe("");
    expect(validateLesson(mapped)).toEqual(expect.arrayContaining([expect.objectContaining({ field: "tradition" })]));
  });

  it("treats a potion-type legacy record as requiring the disclosure", () => {
    expect(toCanonicalLesson("p", { title: "Moon water", type: "potion" }).lesson.requiresPotionDisclosure).toBe(true);
  });
});
