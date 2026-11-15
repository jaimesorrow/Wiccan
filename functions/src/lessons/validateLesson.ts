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
  const tools = Array.isArray(lesson.tools) ? lesson.tools : [];
  return [
    lesson.title, lesson.summary, lesson.introduction, lesson.lesson, lesson.incantation, lesson.reflection,
    lesson.practice, lesson.safetyNote, ...tools.map((tool) => {
      if (!tool || typeof tool !== "object") return "";
      const record = tool as unknown as Record<string, unknown>;
      return `${typeof record.name === "string" ? record.name : ""} ${typeof record.substitution === "string" ? record.substitution : ""}`;
    }),
  ].map((value) => typeof value === "string" ? value : "").join("\n").toLowerCase();
}

/** Matches a phrase unless a nearby prefix negates that specific claim. */
function affirms(text: string, pattern: RegExp): boolean {
  return text.split(/(?<=[.!?\n])\s*/).some((sentence) => {
    const match = new RegExp(pattern.source, pattern.flags.replace("g", "")).exec(sentence);
    if (!match) return false;
    const prefix = sentence.slice(Math.max(0, match.index - 48), match.index);
    return !/\b(?:not|never|no|cannot|can't|won't|doesn't|does not)\s+(?:\w+\s+){0,3}$/i.test(prefix);
  });
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
  { name: "fire", trigger: /\b(candles?|flames?|fires?|burn(?:s|ing)?|embers?|charcoal|incense|matches?|lighters?|smoke)\b/, required: /\b(fire|flame|unattended|heat|burn|extinguish|ventilat)/ },
  { name: "smoke", trigger: /\b(incense|smoke|smudg(?:e|ing)?|burning herbs?|resins?)\b/, required: /\b(ventilat|smoke|airflow|open (a )?window|asthma|respirat)/ },
  { name: "skin contact", trigger: /\b(skin|anoint|apply to|rub on|baths?|bathe|massage|oils?)\b/, required: /\b(skin|patch test|irritat|sensitiv|dilut)/ },
  { name: "ingestion", trigger: /\b(drink|sip|teas?|edible|eat|consume|ingest|infusions?)\b/, required: /\b(consum|drink|ingest|food[- ]safe|edible|do not (eat|drink))/ },
  { name: "children", trigger: /\b(candles?|flames?|fires?|oils?|herbs?|knives|knife|blades?|scissors|cords?|strings?|small (stones?|crystals?)|jars?)\b/, required: /\b(child|children|kid)/ },
  { name: "animals", trigger: /\b(candles?|flames?|oils?|essential oils?|herbs?|plants?|cords?|strings?|small (stones?|crystals?)|incense)\b/, required: /\b(pet|pets|animal|animals|cat|dog)/ },
  { name: "allergies", trigger: /\b(herbs?|plants?|flowers?|petals?|oils?|incense|resins?|baths?|teas?)\b/, required: /\b(allerg|sensitiv)/ },
  { name: "sharp tools", trigger: /\b(knives|knife|blades?|athames?|scissors|needles?|pins?|carve|carving)\b/, required: /\b(sharp|cut|blade|knife|careful)/ },
];

const REVIEWED_FOOD_SAFE_PREPARATION = /\b(?:this|the) (?:preparation|infusion|tea|potion|recipe) (?:has been |is )reviewed as (?:food[- ]safe|edible) for (?:its|the) intended use\b/;
const CONSENT_WAIVER = /\b(?:consent (?:is )?not required|without (?:their )?consent|regardless of consent)\b/;
const AFFIRMATIVE_CONSENT = /\b(?:consent is required|requires? consent|obtain (?:their )?consent|ask for (?:their )?consent|confirm (?:their )?consent|with (?:their|the person's|that person's) consent)\b/;

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
  const secondaryCategoryIds = Array.isArray(lesson.secondaryCategoryIds) ? lesson.secondaryCategoryIds : [];
  if (!Array.isArray(lesson.secondaryCategoryIds)) schema("secondaryCategoryIds", "secondaryCategoryIds must be a list");
  secondaryCategoryIds.forEach((id) => {
    if (!isLessonCategoryId(id)) schema("secondaryCategoryIds", `unknown category "${id}"`);
    if (id === lesson.primaryCategoryId) schema("secondaryCategoryIds", "secondary categories must not repeat the primary");
  });
  if (!(LESSON_TYPES as readonly string[]).includes(lesson.lessonType)) schema("lessonType", `unsupported lessonType "${lesson.lessonType}"`);
  if (!(DIFFICULTIES as readonly string[]).includes(lesson.difficulty)) schema("difficulty", `unsupported difficulty "${lesson.difficulty}"`);
  if (!(CONTENT_STATUSES as readonly string[]).includes(lesson.contentStatus)) schema("contentStatus", `unsupported contentStatus "${lesson.contentStatus}"`);
  if (!Number.isInteger(lesson.durationMinutes) || lesson.durationMinutes < 1 || lesson.durationMinutes > 240) {
    schema("durationMinutes", "durationMinutes must be a whole number of minutes between 1 and 240");
  }
  const lessonText = typeof lesson.lesson === "string" ? lesson.lesson : "";
  const steps = lessonText.split(/\n\s*\n|\n(?=\s*\d+[.)]\s)/).map((s) => s.trim()).filter(Boolean);
  if (lessonText.trim() && steps.length < 3) schema("lesson", "the guided lesson needs at least three distinct steps");
  const tools = Array.isArray(lesson.tools) ? lesson.tools : [];
  if (!Array.isArray(lesson.tools)) schema("tools", "tools must be a list");
  tools.forEach((tool, index) => {
    if (!tool || typeof tool !== "object" || typeof tool.name !== "string" || !tool.name.trim()) {
      schema(`tools[${index}].name`, "every tool needs a name");
    }
  });
  if (!Array.isArray(lesson.tags) || lesson.tags.length === 0) schema("tags", "at least one tag is required");

  // Potion disclosure.
  const potionLike = POTION_LESSON_TYPES.has(lesson.lessonType) || lesson.primaryCategoryId === "potions-infusions";
  if (potionLike && !lesson.requiresPotionDisclosure) {
    safety("requiresPotionDisclosure", "potion, infusion, bath, oil, spray and jar lessons must set requiresPotionDisclosure");
  }
  const text = fullText(lesson);
  const note = typeof lesson.safetyNote === "string" ? lesson.safetyNote.toLowerCase() : "";
  if (potionLike && !affirms(text, REVIEWED_FOOD_SAFE_PREPARATION) && !text.includes(RITUAL_ONLY_LABEL)) {
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
  if (/\b(another person|someone else|your partner|a friend|loved one)\b/.test(text) &&
      (CONSENT_WAIVER.test(note) || !AFFIRMATIVE_CONSENT.test(note))) {
    safety("safetyNote", "a practice that could affect another person must state that consent is required");
  }

  // Sources and attribution.
  if (affirms(text, FAKE_ATTRIBUTION)) source("introduction", "do not invent ancient Wiccan sayings or claim an ancient Wiccan lineage");
  const citations = Array.isArray(lesson.citations) ? lesson.citations : [];
  if (!Array.isArray(lesson.citations)) schema("citations", "citations must be a list");
  citations.forEach((citation, index) => {
    if (!citation || typeof citation !== "object") {
      schema(`citations[${index}]`, "a citation must be an object");
      return;
    }
    if (typeof citation.title !== "string" || !citation.title.trim() || typeof citation.author !== "string" || !citation.author.trim()) source(`citations[${index}]`, "a citation needs a title and an author or institution");
    if (typeof citation.usageNote !== "string" || !citation.usageNote.trim()) source(`citations[${index}].usageNote`, "a citation must say which factual claim it supports");
    if (typeof citation.accessedAt !== "string" || !/^\d{4}-\d{2}-\d{2}$/.test(citation.accessedAt)) source(`citations[${index}].accessedAt`, "a citation needs an access date (YYYY-MM-DD)");
    if (citation.url && (typeof citation.url !== "string" || !/^https:\/\/[^\s]+$/.test(citation.url))) source(`citations[${index}].url`, "citation URLs must be https");
  });
  const quoted = typeof lesson.introduction === "string" ? lesson.introduction.match(/[“"][^”"]{25,}[”"]/g) ?? [] : [];
  if (quoted.length && citations.length === 0) {
    source("introduction", "a quotation needs a citation that identifies its source");
  }
  return issues;
}
