import { isLessonCategoryId } from "./categories";
import {
  CanonicalLesson,
  CONTENT_STATUSES,
  DIFFICULTIES,
  LESSON_TYPES,
  POTION_LESSON_TYPES,
} from "./canonicalLesson";

export type ValidationCode =
  | "schema"
  | "safety"
  | "source";

export interface ValidationIssue {
  code: ValidationCode;
  field: string;
  message: string;
}

const REQUIRED_TEXT: Array<keyof CanonicalLesson> = [
  "id", "title", "slug", "tradition", "summary", "introduction", "lesson", "reflection", "practice", "safetyNote",
];

const RITUAL_ONLY_LABEL = "ritual use only. do not consume";

function fullText(lesson: CanonicalLesson): string {
  return [
    lesson.title, lesson.summary, lesson.introduction, lesson.lesson, lesson.incantation, lesson.reflection,
    lesson.practice, lesson.safetyNote, ...lesson.tools.map((t) => `${t.name} ${t.substitution}`),
  ].join("\n").toLowerCase();
}

/** Matches a phrase only when it is not negated in the same sentence ("does not guarantee" is fine). */
function affirms(text: string, pattern: RegExp): boolean {
  return text.split(/(?<=[.!?\n])\s*/).some((sentence) =>
    pattern.test(sentence) && !/\b(not|never|no|cannot|can't|won't|doesn't|does not|without)\b/.test(sentence));
}

const GUARANTEE = /\b(guarantee[sd]?|will (definitely|certainly|surely|always) (work|bring|make|cause)|100% effective|works every time|is certain to|instantly (attracts?|brings?|heals?|removes?))\b/;
const COERCION = /\b(make (him|her|them|someone|a person) (love|want|return|obey|fall)|bind (him|her|them|your (ex|lover|partner))|control (another|someone|their) (mind|will|feelings)|against (his|her|their) will|without (his|her|their) (knowledge|consent)|force (him|her|them|someone) to)\b/;
const MEDICAL = /\b(cures?|treats?|heals?) (cancer|disease|illness|infection|depression|anxiety|diabetes|covid|wounds?)\b|\b(instead of|replace[sd]?|stop taking) (your )?(medication|medicine|doctor|treatment|therapy)\b/;
const OIL_INGESTION = /\b(drink|ingest|swallow|consume|add to (tea|water|food|a drink)|take internally)\b[^.\n]*\bessential oils?\b|\bessential oils?\b[^.\n]*\b(drink|ingest|swallow|consume|take internally|add to (tea|water|food|a drink))\b/;
const FAKE_ATTRIBUTION = /\b(an ancient|the ancient|old|ancient) (wiccan|witch(es)?'?s?) (saying|proverb|teaching)|as the ancient wiccans|wicca has been practi[cs]ed for (thousands|millennia)/;

interface SafetyTrigger {
  name: string;
  trigger: RegExp;
  /** The safety note must address this concern in one of these terms. */
  required: RegExp;
}

const SAFETY_TRIGGERS: SafetyTrigger[] = [
  { name: "fire", trigger: /\b(candle|flame|fire|burn|ember|charcoal|incense|match(es)?|lighter|smoke)\b/, required: /\b(fire|flame|unattended|heat|burn|extinguish|ventilat)/ },
  { name: "smoke", trigger: /\b(incense|smoke|smudg|burning herbs?|resin)\b/, required: /\b(ventilat|smoke|airflow|open (a )?window|asthma|respirat)/ },
  { name: "skin contact", trigger: /\b(skin|anoint|apply to|rub on|bath|bathe|massage|oil)\b/, required: /\b(skin|patch test|irritat|sensitiv|dilut)/ },
  { name: "ingestion", trigger: /\b(drink|sip|tea|edible|eat|consume|ingest|infusion)\b/, required: /\b(consum|drink|ingest|food[- ]safe|edible|do not (eat|drink))/ },
  { name: "children", trigger: /\b(candle|flame|fire|oil|herb|knife|blade|scissors|cord|string|small (stones?|crystals?)|jar)\b/, required: /\b(child|children|kid)/ },
  { name: "animals", trigger: /\b(candle|flame|oil|essential oil|herb|plant|cord|string|small (stones?|crystals?)|incense)\b/, required: /\b(pet|pets|animal|animals|cat|dog)/ },
  { name: "allergies", trigger: /\b(herb|plant|flower|petal|oil|incense|resin|bath|tea)\b/, required: /\b(allerg|sensitiv)/ },
  { name: "sharp tools", trigger: /\b(knife|blade|athame|scissors|needle|pin|carve|carving)\b/, required: /\b(sharp|cut|blade|knife|careful)/ },
];

/**
 * Schema, safety and source validation. Every issue blocks the lesson: a
 * draft is only accepted with an empty list. Passing never replaces the
 * human review required before publication.
 */
export function validateLesson(lesson: CanonicalLesson): ValidationIssue[] {
  const issues: ValidationIssue[] = [];
  const schema = (field: string, message: string) => issues.push({ code: "schema", field, message });
  const safety = (field: string, message: string) => issues.push({ code: "safety", field, message });
  const source = (field: string, message: string) => issues.push({ code: "source", field, message });

  for (const field of REQUIRED_TEXT) {
    const value = lesson[field];
    if (typeof value !== "string" || !value.trim() || value.trim().toLowerCase() === "null") {
      schema(field, `${field} is required`);
    }
  }
  if (lesson.slug && !/^[a-z0-9]+(-[a-z0-9]+)*$/.test(lesson.slug)) schema("slug", "slug must be lowercase words joined by hyphens");
  if (!isLessonCategoryId(lesson.primaryCategoryId)) schema("primaryCategoryId", `unknown category "${lesson.primaryCategoryId}"`);
  lesson.secondaryCategoryIds.forEach((id) => {
    if (!isLessonCategoryId(id)) schema("secondaryCategoryIds", `unknown category "${id}"`);
    if (id === lesson.primaryCategoryId) schema("secondaryCategoryIds", "secondary categories must not repeat the primary");
  });
  if (!(LESSON_TYPES as readonly string[]).includes(lesson.lessonType)) schema("lessonType", `unsupported lessonType "${lesson.lessonType}"`);
  if (!(DIFFICULTIES as readonly string[]).includes(lesson.difficulty)) schema("difficulty", `unsupported difficulty "${lesson.difficulty}"`);
  if (!(CONTENT_STATUSES as readonly string[]).includes(lesson.contentStatus)) schema("contentStatus", `unsupported contentStatus "${lesson.contentStatus}"`);
  if (!Number.isInteger(lesson.durationMinutes) || lesson.durationMinutes < 1 || lesson.durationMinutes > 240) {
    schema("durationMinutes", "durationMinutes must be a whole number of minutes between 1 and 240");
  }
  const steps = lesson.lesson.split(/\n\s*\n|\n(?=\s*\d+[.)]\s)/).map((s) => s.trim()).filter(Boolean);
  if (lesson.lesson.trim() && steps.length < 3) schema("lesson", "the guided lesson needs at least three distinct steps");
  lesson.tools.forEach((tool, index) => {
    if (!tool.name.trim()) schema(`tools[${index}].name`, "every tool needs a name");
  });
  if (lesson.tags.length === 0) schema("tags", "at least one tag is required");

  // Potion disclosure.
  const potionLike = POTION_LESSON_TYPES.has(lesson.lessonType) || lesson.primaryCategoryId === "potions-infusions";
  if (potionLike && !lesson.requiresPotionDisclosure) {
    safety("requiresPotionDisclosure", "potion, infusion, bath, oil, spray and jar lessons must set requiresPotionDisclosure");
  }
  const text = fullText(lesson);
  const note = lesson.safetyNote.toLowerCase();
  if (potionLike && !/\b(food[- ]safe|edible)\b/.test(text) && !text.includes(RITUAL_ONLY_LABEL)) {
    safety("lesson", 'a preparation that is not reviewed as food-safe must be labelled "Ritual use only. Do not consume."');
  }

  // Content safety.
  if (OIL_INGESTION.test(text) && affirms(text, OIL_INGESTION)) safety("lesson", "never recommend ingesting essential oils");
  if (affirms(text, GUARANTEE)) safety("lesson", "lessons must not promise supernatural or guaranteed outcomes");
  if (affirms(text, COERCION)) safety("lesson", "coercive or non-consensual practices are not allowed");
  if (affirms(text, MEDICAL)) safety("lesson", "lessons must not make medical treatment claims");
  for (const rule of SAFETY_TRIGGERS) {
    if (rule.trigger.test(text) && !rule.required.test(note)) {
      safety("safetyNote", `the safety note must address ${rule.name}`);
    }
  }
  if (/\b(another person|someone else|your partner|a friend|loved one)\b/.test(text) && !/\bconsent\b/.test(note)) {
    safety("safetyNote", "a practice that could affect another person must state that consent is required");
  }

  // Sources and attribution.
  if (affirms(text, FAKE_ATTRIBUTION)) source("introduction", "do not invent ancient Wiccan sayings or claim an ancient Wiccan lineage");
  lesson.citations.forEach((citation, index) => {
    if (!citation.title.trim() || !citation.author.trim()) source(`citations[${index}]`, "a citation needs a title and an author or institution");
    if (!citation.usageNote.trim()) source(`citations[${index}].usageNote`, "a citation must say which factual claim it supports");
    if (!/^\d{4}-\d{2}-\d{2}$/.test(citation.accessedAt)) source(`citations[${index}].accessedAt`, "a citation needs an access date (YYYY-MM-DD)");
    if (citation.url && !/^https:\/\/[^\s]+$/.test(citation.url)) source(`citations[${index}].url`, "citation URLs must be https");
  });
  const quoted = lesson.introduction.match(/[“"][^”"]{25,}[”"]/g) ?? [];
  if (quoted.length && lesson.citations.length === 0) {
    source("introduction", "a quotation needs a citation that identifies its source");
  }
  return issues;
}
