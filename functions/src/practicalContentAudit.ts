import { findPassageViolations } from "./sourcePassages";
import type { SourcePassage } from "./sourcePassages";
import { findPracticalLessonViolations, practicalContentHash } from "./practicalLesson";
import { curriculumConcept, findLexicalDuplicateConcerns } from "./contentSimilarity";

export interface AuditDocument { id: string; fields: Record<string, unknown>; updateTime?: string }
const normalize = (value: unknown): string => typeof value === "string" ? value.trim().replace(/\s+/g, " ").toLowerCase() : "";

/** No writes, reclassification, assumed record count or inferred source approval. */
export function auditPracticalContent(documents: AuditDocument[], passages: SourcePassage[], deliveredIds: ReadonlySet<string> = new Set()) {
  const groups = new Map<string, string[]>();
  for (const { id, fields } of documents) {
    for (const [kind, value] of [["title", fields.title], ["body", fields.lesson ?? fields.body], ["concept", fields.similarityGroupId]]) {
      const normalized = normalize(value);
      if (normalized) groups.set(`${kind}:${normalized}`, [...(groups.get(`${kind}:${normalized}`) ?? []), id]);
    }
  }
  const duplicateConcerns = [...groups.entries()].filter(([, ids]) => ids.length > 1).map(([key, ids]) => ({ kind: key.split(":")[0], key, ids }));
  const lexicalDuplicateConcerns = findLexicalDuplicateConcerns(documents.map(({ id, fields }) => curriculumConcept(id, fields)));
  const duplicateIds = new Set([
    ...duplicateConcerns.flatMap((group) => group.ids),
    ...lexicalDuplicateConcerns.flatMap((group) => [group.leftId, group.rightId]),
  ]);
  const byStatus: Record<string, number> = {};
  const byType: Record<string, number> = {};
  const bySource: Record<string, number> = {};
  const records = documents.map(({ id, fields, updateTime }) => {
    for (const [counts, value] of [[byStatus, fields.status ?? fields.contentStatus], [byType, fields.type], [bySource, fields.sourceId]] as const) {
      const key = typeof value === "string" ? value : "(missing)";
      counts[key] = (counts[key] ?? 0) + 1;
    }
    const passage = passages.find((candidate) => candidate.id === fields.sourcePassageId && candidate.sourceId === fields.sourceId);
    const sourceVerified = !!passage && findPassageViolations(passage).length === 0;
    const violations = findPracticalLessonViolations(fields, passages);
    const disclosureRequired = fields.requiresPotionDisclosure === true || fields.type === "potion" || ["potion", "tea", "bath", "oil", "spray", "jar", "skin-contact"].includes(String(fields.lessonType));
    return {
      id, updateTime: updateTime ?? null,
      title: fields.title ?? null, status: fields.status ?? fields.contentStatus ?? null,
      category: fields.primaryCategoryId ?? fields.category ?? null,
      centralConceptForReview: fields.similarityGroupId ?? fields.title ?? null,
      introductionForReview: fields.introduction ?? fields.intro ?? null,
      sourceId: fields.sourceId ?? null, sourceCitation: fields.sourceCitation ?? null, sourceUrl: fields.sourceUrl ?? null,
      rightsReviewRequired: fields.rightsReviewRequired ?? null, rightsNotes: fields.rightsNotes ?? null,
      sourceVerified, duplicateFlagged: duplicateIds.has(id), disclosureRequired,
      safetyReviewRequired: fields.practicalReview == null || violations.some((issue) => /safety/.test(issue)),
      previouslyDelivered: deliveredIds.has(id), published: fields.status === "published" || fields.contentStatus === "published",
      practicalStandardPassed: violations.length === 0 && fields.rightsReviewRequired !== true,
      reviewHash: practicalContentHash(fields), violations,
      disposition: deliveredIds.has(id) || fields.status === "published" ? "compatibility/versioning review; preserve" : "draft; review required; preserve ID and content",
    };
  });
  const uniqueCitations = new Map<string, { sourceId: unknown; claimedCitation: unknown; sourceUrl: unknown; recordIds: string[]; verificationStatus: string }>();
  for (const record of records) {
    const key = JSON.stringify([record.sourceId, record.sourceCitation]);
    const candidate = uniqueCitations.get(key);
    if (candidate) candidate.recordIds.push(record.id);
    else uniqueCitations.set(key, { sourceId: record.sourceId, claimedCitation: record.sourceCitation, sourceUrl: record.sourceUrl, recordIds: [record.id], verificationStatus: "candidate only; inspect passage, dating, exact edition, translation, rights and witchcraft relevance" });
  }
  return {
    totalRecords: documents.length, byStatus, byType, bySource,
    totals: {
      inventoryInspected: records.length,
      editoriallyQualified: records.filter((r) => r.practicalStandardPassed).length,
      ancientSourceVerified: records.filter((r) => r.sourceVerified).length,
      rewritten: 0,
      duplicateFlagged: duplicateIds.size,
      sourceBlocked: records.filter((r) => !r.sourceVerified).length,
      legacyRightsFlags: records.filter((r) => r.rightsReviewRequired === true).length,
      exactEditionRightsUnverified: records.filter((r) => !r.sourceVerified).length,
      safetyReviewRequired: records.filter((r) => r.safetyReviewRequired).length,
      disclosureRequired: records.filter((r) => r.disclosureRequired).length,
      draft: byStatus.draft ?? 0,
      published: records.filter((r) => r.published).length,
      previouslyDelivered: records.filter((r) => r.previouslyDelivered).length,
      eligibleUnderNewStandard: records.filter((r) => r.published && r.practicalStandardPassed).length,
    },
    caveats: ["Structural inspection is not passage verification or a completed editorial review.", "Duplicate groups are concerns requiring technique/objective/sequence review; no records are merged.", "Safety review required does not mean an activity has been verified unsafe.", "No records were changed, reclassified, rewritten or published."],
    duplicateCoverage: { exactAndLexicalExecuted: true, semanticProvider: "not-configured", semanticExecuted: false },
    duplicateConcerns, lexicalDuplicateConcerns, sourceRegisterCandidates: [...uniqueCitations.values()], records,
  };
}
