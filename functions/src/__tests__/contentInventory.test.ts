import { inspectLessonInventory, InventoryDocument } from "../contentInventory";
import { loadContentSeed } from "../contentSeed";

describe("production lesson inventory", () => {
  const seed = loadContentSeed();
  const documents: InventoryDocument[] = seed.map(({ id, ...fields }) => ({ id, fields }));

  it("counts actual lessons independently of spells and potions and identifies all manifest IDs", () => {
    const report = inspectLessonInventory(documents, seed);
    expect(report.lessonCount).toBe(200);
    expect(report.totalDocuments).toBe(296);
    expect(report.lessonsByStatus).toEqual({ draft: 200 });
    expect(report.missingSeedLessonIds).toEqual([]);
    expect(report.issues).toEqual([]);
  });

  it("rejects a 200-document mixed pool with fewer than 200 lessons", () => {
    const mixed = documents.slice(0, 200);
    const report = inspectLessonInventory(mixed, seed);
    expect(report.lessonCount).toBeLessThan(200);
    expect(report.issues.join(" ")).toContain("actual lesson documents");
    expect(report.missingSeedLessonIds.length).toBeGreaterThan(0);
  });

  it("allows additional valid lessons and reports them separately without deleting them", () => {
    const lesson = documents.find((document) => document.fields.type === "lesson")!;
    const report = inspectLessonInventory([...documents, {
      id: "existing-extra-lesson",
      fields: { ...lesson.fields, title: "A separate approved topic", body: "Distinct independently written historical commentary." },
    }], seed);
    expect(report.lessonCount).toBe(201);
    expect(report.extraLessonIds).toEqual(["existing-extra-lesson"]);
    expect(report.issues).toEqual([]);
  });

  it("reports malformed or untraceable lessons rather than treating a raw count as production readiness", () => {
    const changed = documents.map((document) => document.fields.type === "lesson"
      ? { ...document, fields: { ...document.fields, sourceId: "unknown-source", reflection: " " } }
      : document);
    const report = inspectLessonInventory(changed, seed);
    expect(report.lessonCount).toBe(200);
    expect(report.invalidLessons).toHaveLength(200);
    expect(report.issues.length).toBeGreaterThan(0);
  });

  it("flags a published lesson with outstanding rights review", () => {
    const lesson = documents.find((document) => document.fields.type === "lesson")!;
    const changed = documents.map((document) => document.id === lesson.id
      ? { ...document, fields: { ...document.fields, status: "published", rightsReviewRequired: true } }
      : document);
    expect(inspectLessonInventory(changed, seed).invalidLessons).toContain(`${lesson.id}: published with outstanding rights review.`);
  });

  it("rejects an unrelated curated source attached to a canonical lesson ID", () => {
    const lesson = documents.find((document) => document.fields.type === "lesson")!;
    const changed = documents.map((document) => document.id === lesson.id
      ? { ...document, fields: { ...document.fields, sourceId: "mesopotamian-maqlu" } }
      : document);
    expect(inspectLessonInventory(changed, seed).invalidLessons).toContain(`${lesson.id}: source differs from the checked-in lesson at this ID.`);
  });

  it("does not mistake an existing spell at a lesson ID for the expected lesson", () => {
    const lesson = documents.find((document) => document.fields.type === "lesson")!;
    const changed = documents.map((document) => document.id === lesson.id
      ? { ...document, fields: { ...document.fields, type: "spell" } }
      : document);
    const report = inspectLessonInventory(changed, seed);
    expect(report.lessonCount).toBe(199);
    expect(report.missingSeedLessonIds).toEqual([lesson.id]);
  });

  it("flags duplicate lesson titles and bodies even when the raw lesson count passes", () => {
    const lesson = documents.find((document) => document.fields.type === "lesson")!;
    const report = inspectLessonInventory([...documents, { ...lesson, id: "duplicate-lesson" }], seed);
    expect(report.lessonCount).toBe(201);
    expect(report.invalidLessons.join(" ")).toContain("duplicate title");
    expect(report.invalidLessons.join(" ")).toContain("duplicate body");
  });
});
