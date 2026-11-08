import { createHash } from "node:crypto";
import { stableJson } from "../practicalLesson";
import { CanonicalLesson } from "./canonicalLesson";

// ---------------------------------------------------------------------------
// Normalization
// ---------------------------------------------------------------------------

/** Synonym groups collapse to their first member. */
const SYNONYMS: string[][] = [
  ["cord", "string", "thread", "twine", "yarn"],
  ["candle", "taper", "votive", "tealight"],
  ["cleansing", "clearing", "purification", "purifying", "cleanse", "clear", "purify"],
  ["charm", "talisman", "amulet"],
  ["infusion", "tea", "brew", "tisane"],
  ["grounding", "centering", "steadying", "ground", "center", "steady"],
  ["threshold", "doorway", "entrance", "entry", "door"],
  ["jar", "bottle", "vessel"],
  ["sachet", "pouch", "bag"],
  ["stone", "crystal", "pebble", "rock"],
  ["journal", "notebook", "diary", "grimoire"],
  ["speak", "say", "recite", "chant", "whisper"],
  ["knot", "tie"],
];
const SYNONYM_OF = new Map<string, string>(SYNONYMS.flatMap((group) => group.map((word) => [word, group[0]] as [string, string])));

/** Words that decorate a title or sentence without changing the practice. */
const DECORATIVE = new Set([
  "a", "an", "the", "and", "or", "of", "for", "to", "with", "in", "on", "at", "by", "from", "into", "your", "you", "my",
  "simple", "gentle", "easy", "quick", "little", "small", "sacred", "magical", "mystic", "mystical", "practice", "lesson",
  "ritual", "spell", "working", "rite", "this", "that", "it", "is", "are", "be", "as", "will", "can", "one", "each",
]);

/** Details that never make a practice new on their own; replaced by placeholders before comparison. */
const COLOR = /\b(red|orange|yellow|green|blue|indigo|violet|purple|pink|white|black|brown|gold|golden|silver|grey|gray)\b/g;
const HERB = /\b(rosemary|lavender|sage|thyme|basil|mint|peppermint|chamomile|rose|bay|cinnamon|clove|cloves|mugwort|yarrow|juniper|cedar|pine|lemon balm|calendula|hibiscus|nettle|vervain|rue|salt|sea salt|frankincense|myrrh|sandalwood|jasmine)\b/g;
const STONE = /\b(quartz|amethyst|obsidian|citrine|jasper|agate|carnelian|hematite|tourmaline|selenite|moonstone|labradorite|jade|onyx|garnet|aventurine|fluorite|lapis|malachite)\b/g;
const MOON = /\b(new|full|waxing|waning|first quarter|last quarter|dark|gibbous|crescent) moon\b|\b(waxing|waning|gibbous|crescent)\b/g;
const DAY = /\b(monday|tuesday|wednesday|thursday|friday|saturday|sunday)s?\b/g;
const NUMBER = /\b(\d+|one|two|three|four|five|six|seven|eight|nine|ten|twelve|first|second|third|fourth|fifth)\b/g;

function singular(word: string): string {
  if (word.length > 4 && word.endsWith("ies")) return `${word.slice(0, -3)}y`;
  if (word.length > 3 && word.endsWith("s") && !word.endsWith("ss") && !word.endsWith("us")) return word.slice(0, -1);
  return word;
}

export function normalizeText(value: string): string {
  return value.normalize("NFKD").replace(/[̀-ͯ]/g, "").toLowerCase()
    .replace(/[^a-z0-9\s]/g, " ").replace(/\s+/g, " ").trim();
}

/** Lowercased, singularized, synonym-collapsed tokens with decorative words removed. */
export function conceptTokens(value: string): string[] {
  return normalizeText(value).split(" ").filter(Boolean).map(singular)
    .map((word) => SYNONYM_OF.get(word) ?? word).filter((word) => !DECORATIVE.has(word));
}

/** Replaces colours, herbs, stones, moon phases, days and numbers with placeholders. */
export function neutralize(value: string): string {
  return value.toLowerCase().replace(MOON, " moonphase ").replace(DAY, " weekday ").replace(COLOR, " colour ")
    .replace(HERB, " herb ").replace(STONE, " stone ").replace(NUMBER, " count ");
}

export function normalizedTitle(title: string): string {
  return conceptTokens(neutralize(title)).join(" ");
}

const sha = (value: unknown): string => createHash("sha256").update(typeof value === "string" ? value : stableJson(value)).digest("hex");

// ---------------------------------------------------------------------------
// Concept signature
// ---------------------------------------------------------------------------

export interface ConceptSignature {
  primaryMethod: string;
  secondaryMethods: string[];
  intendedPurpose: string;
  primaryTool: string;
  supportingTools: string[];
  ritualActions: string[];
  setting: string;
  timing: string;
  symbolicStructure: string;
  incantationTheme: string;
  followUpAction: string;
}

const RITUAL_VERBS = [
  "tie", "knot", "untie", "braid", "wrap", "bind", "light", "extinguish", "snuff", "carve", "inscribe", "anoint", "dress",
  "place", "set", "arrange", "hold", "breathe", "speak", "write", "burn", "bury", "pour", "stir", "sprinkle", "scatter",
  "fold", "gather", "cleansing", "charge", "draw", "read", "shuffle", "ask", "close", "open", "cast", "call", "grounding",
  "walk", "bathe", "mix", "steep", "strain", "seal", "carry", "hang", "visualize", "imagine", "meditate", "journal", "record",
  "sweep", "ring", "drum", "offer", "bless", "trace", "touch", "rinse", "soak", "infusion", "cut", "fill", "cover", "lay", "sit",
  "stand", "listen", "observe", "name", "release", "return", "store", "dispose", "pause", "repeat",
];
const RITUAL_VERB_SET = new Set(RITUAL_VERBS.map((verb) => SYNONYM_OF.get(verb) ?? verb));
const SETTINGS: Array<[string, RegExp]> = [
  ["home entrance", /\b(threshold|doorway|front door|entrance|entryway|porch)\b/],
  ["altar", /\baltar\b/],
  ["bath", /\b(bath|tub|shower)\b/],
  ["kitchen", /\b(kitchen|stove|hearth)\b/],
  ["bedroom", /\b(bedroom|bedside|pillow|bed)\b/],
  ["outdoors", /\b(garden|outdoors|outside|yard|park|forest|tree|beach|river)\b/],
  ["window", /\b(window|windowsill|sill)\b/],
  ["workspace", /\b(desk|workspace|office)\b/],
];
const TIMINGS: Array<[string, RegExp]> = [
  ["moon phase", /\bmoonphase\b|\bmoon\b/],
  ["sunrise", /\b(dawn|sunrise|morning)\b/],
  ["sunset", /\b(dusk|sunset|evening|twilight)\b/],
  ["night", /\b(night|midnight|bedtime|before sleep)\b/],
  ["returning home", /\b(return(ing)? home|come home|arrive home|enter(ing)? (your|the) home)\b/],
  ["weekly", /\bweekday\b|\b(weekly|every week)\b/],
  ["seasonal", /\b(sabbat|equinox|solstice|season|beltane|samhain|imbolc|lammas|yule|ostara|litha|mabon)\b/],
];

function firstMatch(text: string, table: Array<[string, RegExp]>): string {
  return table.find(([, pattern]) => pattern.test(text))?.[0] ?? "";
}

/** Words that follow a ritual verb without being its object. */
const NON_OBJECTS = new Set([
  "near", "while", "without", "finished", "short", "loose", "slowly", "gently", "carefully", "again", "once", "back",
  "down", "out", "over", "onto", "around", "under", "beside", "toward", "towards", "before", "after", "until", "then",
  "consider", "choose", "notice", "take", "make", "keep", "let", "use", "look", "what", "which", "who", "where", "when",
  "how", "all", "any", "some", "its", "their", "her", "his", "our", "them", "here", "there", "now", "colour", "herb",
]);

/** Splits a guided lesson into its steps: numbered lines or paragraphs, otherwise sentences. */
export function lessonSteps(lesson: string): string[] {
  const numbered = lesson.split(/\n(?=\s*\d+[.)]\s)/).map((s) => s.trim()).filter(Boolean);
  if (numbered.length >= 3) return numbered;
  const paragraphs = lesson.split(/\n\s*\n/).map((s) => s.trim()).filter(Boolean);
  if (paragraphs.length >= 3) return paragraphs;
  return lesson.split(/(?<=[.!?])\s+/).map((s) => s.trim()).filter(Boolean);
}

/** "verb object" for each step: the first ritual verb and the first meaningful word after it. */
export function ritualActions(lesson: string): string[] {
  const actions: string[] = [];
  for (const step of lessonSteps(lesson)) {
    const tokens = conceptTokens(neutralize(step)).filter((t) => t !== "count");
    const at = tokens.findIndex((token) => RITUAL_VERB_SET.has(token));
    if (at < 0) continue;
    const object = tokens.slice(at + 1).find((token) => !RITUAL_VERB_SET.has(token) && !NON_OBJECTS.has(token) && token.length > 2) ?? "";
    const action = `${tokens[at]} ${object}`.trim();
    if (actions[actions.length - 1] !== action) actions.push(action);
  }
  return actions;
}

const TOOL_DESCRIPTORS = new Set(["colour", "count", "natural", "fiber", "fibre", "piece", "safe", "storage", "length", "dried", "fresh", "clean", "large", "plain"]);

/** The head noun of a tool name ("Natural-fiber cord" -> "cord"); colours and counts never distinguish tools. */
function toolHead(name: string): string {
  const tokens = conceptTokens(neutralize(name)).filter((token) => !TOOL_DESCRIPTORS.has(token));
  return tokens[tokens.length - 1] ?? "";
}

export function conceptSignature(lesson: CanonicalLesson): ConceptSignature {
  const whole = neutralize([lesson.title, lesson.summary, lesson.introduction, lesson.lesson, lesson.practice].join(" ").toLowerCase());
  const required = lesson.tools.filter((tool) => tool.required);
  const toolNames = (required.length ? required : lesson.tools).map((tool) => toolHead(tool.name)).filter(Boolean);
  const purpose = [...new Set(conceptTokens(neutralize([
    lesson.title, lesson.summary, lesson.introduction, lesson.practice,
  ].join(" "))))]
    .filter((token) => !toolNames.includes(token) && !RITUAL_VERB_SET.has(token) && !["count", "colour", "herb", "stone"].includes(token))
    .sort();
  return {
    primaryMethod: lesson.primaryCategoryId,
    secondaryMethods: [...lesson.secondaryCategoryIds].sort(),
    intendedPurpose: purpose.join(" "),
    primaryTool: toolNames[0] ?? "",
    supportingTools: [...new Set(lesson.tools.map((tool) => toolHead(tool.name)).filter((t) => t && t !== toolNames[0]))].sort(),
    ritualActions: ritualActions(lesson.lesson),
    setting: firstMatch(whole, SETTINGS),
    timing: firstMatch(whole, TIMINGS),
    symbolicStructure: /\bcount\b/.test(neutralize(lesson.lesson)) ? "counted sequence" : "single action",
    incantationTheme: conceptTokens(neutralize(lesson.incantation)).filter((t) => t.length > 3).slice(0, 6).sort().join(" "),
    followUpAction: ritualActions(lesson.practice).join(" ; ") || conceptTokens(neutralize(lesson.practice)).slice(0, 6).join(" "),
  };
}

/** Identity of the practice: method, tool, actions (order-free), setting and purpose. */
export function signatureHash(signature: ConceptSignature): string {
  return sha({
    method: signature.primaryMethod,
    tool: signature.primaryTool,
    actions: [...new Set(signature.ritualActions)].sort(),
    setting: signature.setting,
    purpose: signature.intendedPurpose,
  });
}

export interface LessonFingerprints {
  id: string;
  slug: string;
  titleHash: string;
  summaryHash: string;
  contentHash: string;
  signatureHash: string;
}

export function fingerprints(lesson: CanonicalLesson, signature = conceptSignature(lesson)): LessonFingerprints {
  return {
    id: lesson.id,
    slug: lesson.slug,
    titleHash: sha(normalizedTitle(lesson.title)),
    summaryHash: sha(conceptTokens(neutralize(lesson.summary)).join(" ")),
    contentHash: sha(conceptTokens(neutralize([lesson.introduction, lesson.lesson, lesson.practice].join(" "))).join(" ")),
    signatureHash: signatureHash(signature),
  };
}

// ---------------------------------------------------------------------------
// Similarity
// ---------------------------------------------------------------------------

export function jaccard<T>(left: Iterable<T>, right: Iterable<T>): number {
  const a = new Set(left);
  const b = new Set(right);
  if (!a.size && !b.size) return 0;
  let shared = 0;
  a.forEach((value) => { if (b.has(value)) shared++; });
  return shared / (a.size + b.size - shared);
}

function trigrams(text: string): string[] {
  const padded = ` ${text} `;
  return Array.from({ length: Math.max(0, padded.length - 2) }, (_, i) => padded.slice(i, i + 3));
}

/** Character-trigram Dice coefficient on normalized titles; robust to small wording changes. */
export function titleSimilarity(left: string, right: string): number {
  const a = trigrams(normalizedTitle(left));
  const b = trigrams(normalizedTitle(right));
  if (!a.length || !b.length) return 0;
  const counts = new Map<string, number>();
  a.forEach((g) => counts.set(g, (counts.get(g) ?? 0) + 1));
  let shared = 0;
  b.forEach((g) => { const n = counts.get(g) ?? 0; if (n > 0) { shared++; counts.set(g, n - 1); } });
  return (2 * shared) / (a.length + b.length);
}

/** Longest-common-subsequence similarity of two ordered action lists. */
export function sequenceSimilarity(left: string[], right: string[]): number {
  if (!left.length || !right.length) return 0;
  const dp = Array.from({ length: left.length + 1 }, () => new Array<number>(right.length + 1).fill(0));
  for (let i = 1; i <= left.length; i++) {
    for (let j = 1; j <= right.length; j++) {
      dp[i][j] = left[i - 1] === right[j - 1] ? dp[i - 1][j - 1] + 1 : Math.max(dp[i - 1][j], dp[i][j - 1]);
    }
  }
  return dp[left.length][right.length] / Math.max(left.length, right.length);
}

/** TF-IDF cosine similarity with smoothed IDF over a shared corpus. */
export class TfIdfSpace {
  private readonly idf = new Map<string, number>();
  private readonly docs: number;

  constructor(corpus: string[][]) {
    this.docs = corpus.length;
    const df = new Map<string, number>();
    corpus.forEach((tokens) => new Set(tokens).forEach((t) => df.set(t, (df.get(t) ?? 0) + 1)));
    df.forEach((count, term) => this.idf.set(term, Math.log((1 + this.docs) / (1 + count)) + 1));
  }

  private vector(tokens: string[]): Map<string, number> {
    const tf = new Map<string, number>();
    tokens.forEach((t) => tf.set(t, (tf.get(t) ?? 0) + 1));
    const v = new Map<string, number>();
    tf.forEach((count, term) => v.set(term, count * (this.idf.get(term) ?? Math.log(1 + this.docs) + 1)));
    return v;
  }

  cosine(left: string[], right: string[]): number {
    if (!left.length || !right.length) return 0;
    const a = this.vector(left);
    const b = this.vector(right);
    let dot = 0;
    a.forEach((value, term) => { dot += value * (b.get(term) ?? 0); });
    const norm = (v: Map<string, number>) => Math.sqrt([...v.values()].reduce((sum, x) => sum + x * x, 0));
    return dot / (norm(a) * norm(b) || 1);
  }
}

export interface SimilarityThresholds {
  title: number;
  summary: number;
  instructions: number;
  combined: number;
  ritualActions: number;
  concept: number;
  /** Fraction of a threshold that counts as a "moderate" match; two or more moderate matches reject. */
  moderateFraction: number;
  /** Minimum number of areas in which a lesson must differ from its closest match. */
  minimumNoveltyAreas: number;
}

export const DEFAULT_THRESHOLDS: SimilarityThresholds = {
  title: 0.85,
  summary: 0.82,
  instructions: 0.78,
  combined: 0.82,
  ritualActions: 0.75,
  concept: 0.80,
  moderateFraction: 0.85,
  minimumNoveltyAreas: 4,
};

export interface IndexedLesson {
  lesson: CanonicalLesson;
  /** Where the record came from, e.g. "firestore:content", "repo:ancientContentSeed.json", "run". */
  source: string;
  signature: ConceptSignature;
  fingerprints: LessonFingerprints;
  tokens: { summary: string[]; instructions: string[]; combined: string[]; reflection: string[]; practice: string[]; introduction: string[] };
  safetyConcerns: string[];
}

const SAFETY_TOPICS: Array<[string, RegExp]> = [
  ["fire", /\b(fire|flame|candle)\b/], ["skin", /\b(skin|patch test)\b/], ["ingestion", /\b(consume|drink|ingest)\b/],
  ["smoke", /\b(smoke|incense|ventilat)\b/], ["sharp", /\b(sharp|blade|knife)\b/], ["water", /\b(water|bath)\b/],
  ["outdoor", /\b(outdoor|forag|collect)\b/],
];

export function indexLesson(lesson: CanonicalLesson, source: string): IndexedLesson {
  const signature = conceptSignature(lesson);
  const tok = (value: string) => conceptTokens(neutralize(value));
  return {
    lesson,
    source,
    signature,
    fingerprints: fingerprints(lesson, signature),
    tokens: {
      summary: tok(lesson.summary),
      instructions: tok(lesson.lesson),
      combined: tok([lesson.title, lesson.summary, lesson.introduction, lesson.lesson, lesson.practice].join(" ")),
      reflection: tok(lesson.reflection),
      practice: tok(lesson.practice),
      introduction: tok(lesson.introduction),
    },
    safetyConcerns: SAFETY_TOPICS.filter(([, re]) => re.test(lesson.safetyNote.toLowerCase())).map(([name]) => name),
  };
}

export interface PairScores {
  titleSimilarity: number;
  summarySimilarity: number;
  instructionSimilarity: number;
  combinedSimilarity: number;
  conceptSimilarity: number;
  ritualActionSimilarity: number;
}

export function conceptSimilarity(a: ConceptSignature, b: ConceptSignature): number {
  const eq = (x: string, y: string) => (x && y && x === y ? 1 : 0);
  return 0.15 * eq(a.primaryMethod, b.primaryMethod)
    + 0.2 * eq(a.primaryTool, b.primaryTool)
    + 0.3 * Math.max(jaccard(a.ritualActions, b.ritualActions), sequenceSimilarity(a.ritualActions, b.ritualActions))
    + 0.15 * jaccard(a.intendedPurpose.split(" ").filter(Boolean), b.intendedPurpose.split(" ").filter(Boolean))
    + 0.1 * eq(a.setting, b.setting)
    + 0.1 * jaccard(a.followUpAction.split(" ").filter(Boolean), b.followUpAction.split(" ").filter(Boolean));
}

export function scorePair(space: TfIdfSpace, a: IndexedLesson, b: IndexedLesson): PairScores {
  return {
    titleSimilarity: titleSimilarity(a.lesson.title, b.lesson.title),
    summarySimilarity: space.cosine(a.tokens.summary, b.tokens.summary),
    instructionSimilarity: space.cosine(a.tokens.instructions, b.tokens.instructions),
    combinedSimilarity: space.cosine(a.tokens.combined, b.tokens.combined),
    conceptSimilarity: conceptSimilarity(a.signature, b.signature),
    ritualActionSimilarity: Math.max(
      jaccard(a.signature.ritualActions, b.signature.ritualActions),
      sequenceSimilarity(a.signature.ritualActions, b.signature.ritualActions),
    ),
  };
}

/** Areas in which the candidate differs from another lesson (novelty requirement). */
export function distinctAreas(space: TfIdfSpace, a: IndexedLesson, b: IndexedLesson, scores: PairScores): string[] {
  const listDiffers = (left: string[], right: string[]) =>
    (left.length > 0 || right.length > 0) && jaccard(left, right) < 0.5;
  const leftPurpose = a.signature.intendedPurpose.split(" ").filter(Boolean);
  const rightPurpose = b.signature.intendedPurpose.split(" ").filter(Boolean);
  const areas: Array<[boolean, string]> = [
    [a.signature.primaryMethod !== b.signature.primaryMethod, "Uses a different core method"],
    [scores.ritualActionSimilarity < 0.5, "Uses a different ritual action sequence"],
    [a.signature.primaryTool !== b.signature.primaryTool, "Teaches a different practical skill"],
    [a.signature.setting !== b.signature.setting, "Takes place in a different setting"],
    [a.signature.symbolicStructure !== b.signature.symbolicStructure, "Has a different symbolic structure"],
    [listDiffers(a.signature.supportingTools, b.signature.supportingTools), "Uses tools differently"],
    [a.signature.timing !== b.signature.timing, "Has different timing"],
    [space.cosine(a.tokens.reflection, b.tokens.reflection) < 0.5, "Takes a different reflection approach"],
    [space.cosine(a.tokens.practice, b.tokens.practice) < 0.5, "Has a different follow-up practice"],
    [listDiffers(a.safetyConcerns, b.safetyConcerns), "Introduces different safety considerations"],
    [space.cosine(a.tokens.introduction, b.tokens.introduction) < 0.5, "Draws on different context"],
    [listDiffers(leftPurpose, rightPurpose), "Practices a different behavior or purpose"],
  ];
  return areas.filter(([different]) => different).map(([, label]) => label);
}

export interface DuplicateReport {
  candidateId: string;
  status: "accepted-draft" | "rejected-duplicate";
  closestMatchId: string | null;
  closestMatchTitle: string | null;
  closestMatchSource: string | null;
  titleSimilarity: number;
  summarySimilarity: number;
  instructionSimilarity: number;
  combinedSimilarity: number;
  conceptSimilarity: number;
  ritualActionSimilarity: number;
  distinctiveFeatures: string[];
  rejectionReasons: string[];
}

const round = (n: number) => Math.round(n * 1000) / 1000;

/**
 * Runs every exact, structural and textual check of a candidate against the
 * inventory (which must already include candidates accepted earlier in the
 * same run). Deterministic: no network, no randomness.
 */
export function checkDuplicates(
  candidate: IndexedLesson,
  inventory: IndexedLesson[],
  thresholds: SimilarityThresholds = DEFAULT_THRESHOLDS,
): DuplicateReport {
  const reasons: string[] = [];
  const space = new TfIdfSpace([candidate, ...inventory].map((item) => item.tokens.combined));
  const fp = candidate.fingerprints;

  for (const other of inventory) {
    const ofp = other.fingerprints;
    const label = `${other.lesson.id} (${other.source})`;
    if (fp.id === ofp.id) reasons.push(`same document ID as ${label}`);
    if (fp.slug && fp.slug === ofp.slug) reasons.push(`same slug as ${label}`);
    if (fp.titleHash === ofp.titleHash) reasons.push(`same normalized title as ${label}`);
    if (fp.summaryHash === ofp.summaryHash) reasons.push(`same normalized summary as ${label}`);
    if (fp.contentHash === ofp.contentHash) reasons.push(`same content as ${label}`);
    if (fp.signatureHash === ofp.signatureHash) reasons.push(`same concept signature as ${label}`);
  }

  let closest: { other: IndexedLesson; scores: PairScores; weight: number } | null = null;
  for (const other of inventory) {
    const scores = scorePair(space, candidate, other);
    const checks: Array<[number, number, string]> = [
      [scores.titleSimilarity, thresholds.title, "title"],
      [scores.summarySimilarity, thresholds.summary, "summary"],
      [scores.instructionSimilarity, thresholds.instructions, "guided instructions"],
      [scores.combinedSimilarity, thresholds.combined, "combined text"],
      [scores.ritualActionSimilarity, thresholds.ritualActions, "ritual action sequence"],
      [scores.conceptSimilarity, thresholds.concept, "concept signature"],
    ];
    const strong = checks.filter(([value, limit]) => value >= limit);
    const moderate = checks.filter(([value, limit]) => value >= limit * thresholds.moderateFraction && value < limit);
    strong.forEach(([value, , name]) => reasons.push(`${name} similarity ${round(value)} with ${other.lesson.id} (${other.source})`));
    if (!strong.length && moderate.length >= 2) {
      reasons.push(`several moderate matches with ${other.lesson.id} (${other.source}): ${moderate.map(([v, , n]) => `${n} ${round(v)}`).join(", ")}`);
    }
    const weight = Math.max(scores.combinedSimilarity, scores.conceptSimilarity, scores.instructionSimilarity);
    if (!closest || weight > closest.weight) closest = { other, scores, weight };
  }

  let distinctive: string[] = [];
  if (closest) {
    distinctive = distinctAreas(space, candidate, closest.other, closest.scores);
    if (distinctive.length < thresholds.minimumNoveltyAreas) {
      reasons.push(`differs from closest match ${closest.other.lesson.id} in only ${distinctive.length} of ${thresholds.minimumNoveltyAreas} required areas`);
    }
  }
  const s = closest?.scores;
  return {
    candidateId: candidate.lesson.id,
    status: reasons.length ? "rejected-duplicate" : "accepted-draft",
    closestMatchId: closest?.other.lesson.id ?? null,
    closestMatchTitle: closest?.other.lesson.title ?? null,
    closestMatchSource: closest?.other.source ?? null,
    titleSimilarity: round(s?.titleSimilarity ?? 0),
    summarySimilarity: round(s?.summarySimilarity ?? 0),
    instructionSimilarity: round(s?.instructionSimilarity ?? 0),
    combinedSimilarity: round(s?.combinedSimilarity ?? 0),
    conceptSimilarity: round(s?.conceptSimilarity ?? 0),
    ritualActionSimilarity: round(s?.ritualActionSimilarity ?? 0),
    distinctiveFeatures: distinctive,
    rejectionReasons: [...new Set(reasons)],
  };
}
