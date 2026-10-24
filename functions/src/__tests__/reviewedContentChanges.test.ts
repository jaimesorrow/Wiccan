import { practicalContentHash } from "../practicalLesson";
import { assertReviewedApply, changeSetDigest, planReviewedChange } from "../reviewedContentChanges";
import type { BackupManifest, ReviewedChangeSet } from "../reviewedContentChanges";
import type { SourcePassage } from "../sourcePassages";

const passage: SourcePassage = {
  id: "passage-1", sourceId: "source-1", textTitle: "Source", manuscriptId: "MS 1",
  tradition: "Medieval ceremonial magic", manuscriptDate: "Fifteenth century", edition: "Edition",
  editionDate: "1900", translator: "Editor", locator: "folio 1r", url: "https://example.org/passage",
  inspectedPassage: "A reviewed passage.", attestedInstructions: ["Prepare the place."], uncertainties: [],
  formula: null, formulaOmissionReason: "No formula.",
  dating: { era: "medieval", status: "verified", evidenceUrl: "https://example.org/catalogue", notes: "Catalogue evidence." },
  rights: { status: "verified", basis: "Public domain.", reference: "https://example.org/rights", jurisdictions: ["US"] },
  scope: { status: "approved", rationale: "Supports a safe space preparation.", evidenceUrl: "https://example.org/scope" },
  review: { passageInspected: true, sourceFidelityApproved: true, safetyApproved: true, reviewedBy: "editor", reviewedAt: "2026-10-23T00:00:00.000Z" },
};

function reviewedFields() {
  const fields: Record<string, unknown> = {
    title: "Prepared Table", type: "lesson", tier: "premium", tradition: "Wiccan / Modern Witchcraft",
    sourceTradition: passage.tradition, summary: "Prepare a clean ritual table.", intro: "A modern adaptation.",
    introduction: "A modern adaptation.", body: "1. Clean the table.\n2. Close the working.",
    lesson: "1. Clean the table.\n2. Close the working.", reflection: "What changed when you prepared the space?",
    practice: "Complete the method once.", materials: ["cloth"], tools: ["cloth"], safetyNote: "Keep the path clear.",
    tags: ["altar"], category: "altars-sacred-space", primaryCategoryId: "altars-sacred-space", secondaryCategoryIds: [],
    lessonType: "altar", requiresPotionDisclosure: false, durationMinutes: 10, incantation: null,
    incantationOmissionReason: "No formula is present.", timing: null, application: "Modern Wiccan sacred-space preparation.",
    correspondences: [], adaptationNotes: ["The practice is a modern adaptation."], sourceId: passage.sourceId,
    sourcePassageId: passage.id, sourceCitation: passage.locator, sourceUrl: passage.url,
    citations: [{ textTitle: passage.textTitle, manuscriptId: passage.manuscriptId, tradition: passage.tradition,
      manuscriptDate: passage.manuscriptDate, compositionDate: "", edition: passage.edition, editionDate: passage.editionDate,
      translator: passage.translator, locator: passage.locator, url: passage.url }], rightsReviewRequired: false, contentStatus: "draft",
  };
  fields.practicalReview = { reviewedBy: "lesson editor", reviewedAt: "2026-10-23T01:00:00.000Z", contentHash: practicalContentHash(fields),
    sourceFidelityApproved: true, scopeApproved: true, safetyApproved: true, rightsApproved: true, duplicateReviewed: true };
  return fields;
}

describe("reviewed migration changes", () => {
  const changeSet: ReviewedChangeSet = { projectId: "wiccan-android", reviewId: "review-1", entries: [{ id: "lesson-1", expectedUpdateTime: "1:1", fields: reviewedFields() }] };
  const backup: BackupManifest = { projectId: "wiccan-android", exportUri: "gs://private/backup", artifactSha256: "a".repeat(64), objectGeneration: "1", completedAt: "2026-10-23T00:00:00.000Z", restoreTestedAt: "2026-10-23T00:30:00.000Z", contentIds: ["lesson-1"] };

  it("requires approval of the exact digest and restore-tested backup", () => {
    expect(() => assertReviewedApply(changeSet, changeSetDigest(changeSet), backup)).not.toThrow();
    expect(() => assertReviewedApply(changeSet, "b".repeat(64), backup)).toThrow("Explicit approval");
  });

  it("blocks delivered and concurrently edited records", () => {
    const current = { status: "draft", sourceId: "source-1" };
    expect(planReviewedChange(current, "1:1", true, changeSet.entries[0], [passage]).outcome).toBe("conflict");
    expect(planReviewedChange(current, "2:2", false, changeSet.entries[0], [passage]).outcome).toBe("conflict");
  });
});
