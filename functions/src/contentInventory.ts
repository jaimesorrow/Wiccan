import { PRACTICAL_CATEGORIES } from "./practicalLesson";
import { z } from "zod";
import { ContentSource, loadContentSources } from "./contentSource";
import { EXPECTED_LESSON_COUNT } from "./contentSeed";
import { CONTENT_CATEGORIES, SeedContent } from "./types";

export interface InventoryDocument {
  id: string;
  fields: Record<string, unknown>;
}

export interface LessonInventory {
  totalDocuments: number;
  lessonCount: number;
  lessonsByStatus: Record<string, number>;
  lessonsByTier: Record<string, number>;
  rightsReviewRequired: number;
  missingSeedLessonIds: string[];
  extraLessonIds: string[];
  invalidLessons: string[];
  issues: string[];
}

const PublishedOrDraftLessonSchema = z.object({
  type: z.literal("lesson"),
  tier: z.enum(["free", "premium"]),
  status: z.enum(["draft", "published"]),
  title: z.string().trim().min(1),
  tradition: z.string().trim().min(1),
  category: z.enum([...CONTENT_CATEGORIES, ...PRACTICAL_CATEGORIES]),
  intro: z.string().trim().min(1),
  body: z.string().trim().min(1),
  reflection: z.string().trim().min(1),
  tags: z.array(z.string().trim().min(1)).min(1),
  sourceId: z.string().trim().min(1),
});

/** Pure verification: the Firestore CLI and tests use the same actual lesson gate. */
export function inspectLessonInventory(
  documents: InventoryDocument[],
  seed: SeedContent[],
  sources: ContentSource[] = loadContentSources(),
): LessonInventory {
  const lessons = documents.filter((document) => document.fields.type === "lesson");
  const lessonIds = new Set(lessons.map((document) => document.id));
  const expectedLessons = new Map(seed.filter((entry) => entry.type === "lesson").map((entry) => [entry.id, entry]));
  const expectedIds = new Set(expectedLessons.keys());
  const lessonsByStatus: Record<string, number> = {};
  const lessonsByTier: Record<string, number> = {};
  const invalidLessons: string[] = [];
  const seenTitles = new Map<string, string>();
  const seenBodies = new Map<string, string>();
  let rightsReviewRequired = 0;

  for (const { id, fields } of lessons) {
    const status = typeof fields.status === "string" ? fields.status : "(missing)";
    const tier = typeof fields.tier === "string" ? fields.tier : "(missing)";
    lessonsByStatus[status] = (lessonsByStatus[status] ?? 0) + 1;
    lessonsByTier[tier] = (lessonsByTier[tier] ?? 0) + 1;
    if (fields.rightsReviewRequired === true) rightsReviewRequired += 1;

    const parsed = PublishedOrDraftLessonSchema.safeParse(fields);
    if (!parsed.success) {
      const paths = parsed.error.issues.map((issue) => issue.path.join("."));
      invalidLessons.push(`${id}: invalid or missing ${paths.join(", ")}.`);
      continue;
    }
    for (const [value, seen, label] of [
      [parsed.data.title, seenTitles, "title"],
      [parsed.data.body, seenBodies, "body"],
    ] as const) {
      const normalized = value.replace(/\s+/g, " ").toLowerCase();
      const duplicateId = seen.get(normalized);
      if (duplicateId) invalidLessons.push(`${id}: duplicate ${label} of lesson '${duplicateId}'.`);
      seen.set(normalized, id);
    }
    const source = sources.find((candidate) => candidate.id === parsed.data.sourceId);
    if (!source || source.era !== "ancient" || !source.eligibleTypes.includes("lesson") || !source.groundedFacts.length) {
      invalidLessons.push(`${id}: source is not a curated ancient, lesson-eligible source.`);
    }
    const expectedLesson = expectedLessons.get(id);
    if (expectedLesson && parsed.data.sourceId !== expectedLesson.sourceId) {
      invalidLessons.push(`${id}: source differs from the checked-in lesson at this ID.`);
    }
    if (!parsed.data.reflection.endsWith("?")) {
      invalidLessons.push(`${id}: reflection is not a question.`);
    }
    if (status === "published" && fields.rightsReviewRequired === true) {
      invalidLessons.push(`${id}: published with outstanding rights review.`);
    }
  }

  const missingSeedLessonIds = [...expectedIds].filter((id) => !lessonIds.has(id));
  const extraLessonIds = [...lessonIds].filter((id) => !expectedIds.has(id));
  const issues: string[] = [];
  if (lessons.length < EXPECTED_LESSON_COUNT) {
    issues.push(`Expected at least ${EXPECTED_LESSON_COUNT} actual lesson documents; found ${lessons.length}.`);
  }
  if (missingSeedLessonIds.length > 0) {
    issues.push(`${missingSeedLessonIds.length} checked-in lesson IDs are absent or have another type.`);
  }
  if (invalidLessons.length > 0) issues.push(...invalidLessons);

  return {
    totalDocuments: documents.length,
    lessonCount: lessons.length,
    lessonsByStatus,
    lessonsByTier,
    rightsReviewRequired,
    missingSeedLessonIds,
    extraLessonIds,
    invalidLessons,
    issues,
  };
}

