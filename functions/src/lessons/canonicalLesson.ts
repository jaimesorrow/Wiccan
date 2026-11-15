import {
  inferCategoryFromText,
  isLessonCategoryId,
  LEGACY_INTENTION_CATEGORIES,
  LEGACY_PRACTICAL_CATEGORY_MAP,
  LessonCategoryId,
} from "./categories";

export const LESSON_TYPES = ["guided-practice", "spell", "ritual", "potion", "infusion", "bath", "oil", "spray", "jar"] as const;
export type LessonType = (typeof LESSON_TYPES)[number];
/** Lesson types that always involve a preparation and therefore the potion safety disclosure. */
export const POTION_LESSON_TYPES: ReadonlySet<string> = new Set(["potion", "infusion", "bath", "oil", "spray", "jar"]);

export const DIFFICULTIES = ["accessible", "intermediate", "advanced"] as const;
export type Difficulty = (typeof DIFFICULTIES)[number];

export const CONTENT_STATUSES = ["draft", "review", "rejected", "archived", "published"] as const;
export type LessonContentStatus = (typeof CONTENT_STATUSES)[number];

export interface LessonTool {
  name: string;
  quantity: string;
  required: boolean;
  substitution: string;
}

export interface LessonCorrespondences {
  elements: string[];
  colors: string[];
  herbs: string[];
  stones: string[];
  moonPhases: string[];
  days: string[];
  symbols: string[];
}

export interface LessonCitation {
  title: string;
  author: string;
  edition?: string;
  url?: string;
  accessedAt: string;
  /** The specific factual statement this source supports. */
  usageNote: string;
  rightsNote?: string;
}

/**
 * The single canonical lesson shape (docs/CONTENT_SCHEMA_V2.md). Firestore
 * keeps the legacy fields the current app reads (intro, body, materials,
 * category, status) alongside these until every reader uses this model.
 */
export interface CanonicalLesson {
  id: string;
  title: string;
  slug: string;
  tradition: string;
  summary: string;
  introduction: string;
  lesson: string;
  incantation: string;
  reflection: string;
  practice: string;
  safetyNote: string;
  primaryCategoryId: LessonCategoryId;
  secondaryCategoryIds: LessonCategoryId[];
  lessonType: LessonType;
  requiresPotionDisclosure: boolean;
  difficulty: Difficulty;
  durationMinutes: number;
  tools: LessonTool[];
  correspondences: LessonCorrespondences;
  tags: string[];
  citations: LessonCitation[];
  contentStatus: LessonContentStatus;
}

export const EMPTY_CORRESPONDENCES: LessonCorrespondences = {
  elements: [], colors: [], herbs: [], stones: [], moonPhases: [], days: [], symbols: [],
};

export function slugify(title: string): string {
  return title.normalize("NFKD").replace(/[̀-ͯ]/g, "").toLowerCase()
    .replace(/[^a-z0-9]+/g, "-").replace(/^-+|-+$/g, "");
}

const str = (value: unknown): string => (typeof value === "string" ? value.trim() : "");
const strList = (value: unknown): string[] =>
  Array.isArray(value) ? value.filter((v): v is string => typeof v === "string" && v.trim().length > 0).map((v) => v.trim()) : [];

export interface CompatibilityResult {
  lesson: CanonicalLesson;
  /** Every place the legacy document could not be mapped faithfully; empty when mapping is exact. */
  issues: string[];
}

function mapCategory(fields: Record<string, unknown>, text: string, issues: string[]): { primary: LessonCategoryId; tags: string[] } {
  const declared = str(fields.primaryCategoryId) || str(fields.category);
  if (isLessonCategoryId(declared)) return { primary: declared, tags: [] };
  const legacyPractical = LEGACY_PRACTICAL_CATEGORY_MAP[declared];
  if (legacyPractical) return { primary: legacyPractical, tags: [] };
  const intention = LEGACY_INTENTION_CATEGORIES[declared];
  const inferred = inferCategoryFromText(text);
  if (intention) {
    if (!inferred) issues.push(`category "${declared}" is an intention; no method named in the text, used fallback ${intention.fallback}`);
    return { primary: inferred ?? intention.fallback, tags: [intention.tag] };
  }
  if (inferred) {
    issues.push(`unknown category "${declared || "(none)"}"; inferred ${inferred} from the text`);
    return { primary: inferred, tags: [] };
  }
  issues.push(`unknown category "${declared || "(none)"}" and no method in the text; needs an editor`);
  return { primary: "altars-sacred-space", tags: [] };
}

function mapTools(fields: Record<string, unknown>): LessonTool[] {
  if (Array.isArray(fields.tools) && fields.tools.every((t) => t && typeof t === "object")) {
    return (fields.tools as Record<string, unknown>[]).map((t) => ({
      name: str(t.name), quantity: str(t.quantity), required: t.required !== false, substitution: str(t.substitution),
    })).filter((t) => t.name);
  }
  const names = strList(fields.tools).length ? strList(fields.tools) : strList(fields.materials);
  return names.filter((n) => n.toLowerCase() !== "none").map((name) => ({ name, quantity: "", required: true, substitution: "" }));
}

function mapCorrespondences(value: unknown): LessonCorrespondences {
  if (value && typeof value === "object" && !Array.isArray(value)) {
    const v = value as Record<string, unknown>;
    return {
      elements: strList(v.elements), colors: strList(v.colors), herbs: strList(v.herbs), stones: strList(v.stones),
      moonPhases: strList(v.moonPhases), days: strList(v.days), symbols: strList(v.symbols),
    };
  }
  // Legacy free-text correspondences are kept as symbols rather than guessed into typed buckets.
  return { ...EMPTY_CORRESPONDENCES, symbols: strList(value) };
}

function mapCitations(fields: Record<string, unknown>, issues: string[]): LessonCitation[] {
  if (Array.isArray(fields.citations) && fields.citations.length) {
    return (fields.citations as Record<string, unknown>[]).map((c) => ({
      title: str(c.title ?? c.textTitle), author: str(c.author ?? c.institution), edition: str(c.edition) || undefined,
      url: str(c.url) || undefined, accessedAt: str(c.accessedAt), usageNote: str(c.usageNote),
      rightsNote: str(c.rightsNote ?? c.rights) || undefined,
    }));
  }
  const citation = str(fields.sourceCitation);
  if (!citation) return [];
  issues.push("legacy sourceCitation has no usageNote or access date; citation needs editor review");
  return [{ title: citation, author: "", url: str(fields.sourceUrl) || undefined, accessedAt: "", usageNote: "" }];
}

function mapLessonType(fields: Record<string, unknown>): LessonType {
  const declared = str(fields.lessonType);
  if ((LESSON_TYPES as readonly string[]).includes(declared)) return declared as LessonType;
  if (["tea"].includes(declared)) return "infusion";
  if (fields.type === "potion") return "potion";
  if (fields.type === "spell") return "spell";
  return "guided-practice";
}

/**
 * Maps any stored lesson document (legacy or canonical) to the canonical
 * model without changing its document ID. Mapping never invents content: a
 * missing field stays empty and is reported, so validation fails it safely.
 */
export function toCanonicalLesson(id: string, fields: Record<string, unknown>): CompatibilityResult {
  const issues: string[] = [];
  const title = str(fields.title);
  const introduction = str(fields.introduction) || str(fields.intro);
  const lesson = str(fields.lesson) || str(fields.body);
  const text = [title, introduction, lesson].join(" ");
  const { primary, tags: categoryTags } = mapCategory(fields, text, issues);
  const secondary = strList(fields.secondaryCategoryIds)
    .map((c) => (isLessonCategoryId(c) ? c : LEGACY_PRACTICAL_CATEGORY_MAP[c]))
    .filter((c): c is LessonCategoryId => !!c && c !== primary);
  const lessonType = mapLessonType(fields);
  const status = str(fields.contentStatus) || str(fields.status) || "draft";
  if (!(CONTENT_STATUSES as readonly string[]).includes(status)) issues.push(`unsupported status "${status}"; treated as draft`);
  const summary = str(fields.summary);
  if (!summary) issues.push("no summary");
  const duration = typeof fields.durationMinutes === "number" && fields.durationMinutes > 0 ? fields.durationMinutes : 0;
  if (!duration) issues.push("no durationMinutes");
  const difficulty = (DIFFICULTIES as readonly string[]).includes(str(fields.difficulty)) ? (str(fields.difficulty) as Difficulty) : "accessible";

  const result: CanonicalLesson = {
    id,
    title,
    slug: str(fields.slug) || slugify(title),
    tradition: str(fields.tradition),
    summary,
    introduction,
    lesson,
    incantation: str(fields.incantation),
    reflection: str(fields.reflection),
    practice: str(fields.practice),
    safetyNote: str(fields.safetyNote),
    primaryCategoryId: primary,
    secondaryCategoryIds: [...new Set(secondary)],
    lessonType,
    requiresPotionDisclosure: fields.requiresPotionDisclosure === true || POTION_LESSON_TYPES.has(lessonType),
    difficulty,
    durationMinutes: duration,
    tools: mapTools(fields),
    correspondences: mapCorrespondences(fields.correspondences),
    tags: [...new Set([...strList(fields.tags), ...categoryTags])],
    citations: mapCitations(fields, issues),
    contentStatus: (CONTENT_STATUSES as readonly string[]).includes(status) ? (status as LessonContentStatus) : "draft",
  };
  return { lesson: result, issues };
}

/**
 * The legacy fields today's app and getTodayContent read, derived from a
 * canonical lesson so new drafts stay readable before the app migrates.
 * `status` stays "draft" unless the lesson is published.
 */
export function legacyFieldsFor(lesson: CanonicalLesson): Record<string, unknown> {
  return {
    title: lesson.title,
    intro: lesson.introduction,
    body: lesson.lesson,
    materials: lesson.tools.map((tool) => tool.name),
    category: lesson.primaryCategoryId,
    type: lesson.requiresPotionDisclosure ? "potion" : lesson.lessonType === "spell" ? "spell" : "lesson",
    tier: "premium",
    status: lesson.contentStatus === "published" ? "published" : "draft",
  };
}
