/**
 * The ten practical categories. A lesson's primary category names the
 * method it teaches, never the outcome it is for: intentions such as love,
 * protection or prosperity belong in tags. IDs are stable and stored in
 * Firestore; change a label freely, never an ID.
 */
export const LESSON_CATEGORIES = [
  { id: "candle-spells", label: "Candle Spells", description: "Workings built around lighting, dressing, carving or tending a candle flame." },
  { id: "herbal-rituals", label: "Herbal Rituals", description: "Rituals that handle dried or fresh plant material: bundles, sachets, offerings, strewing." },
  { id: "cord-knot-work", label: "Cord & Knot Work", description: "Tying, braiding, binding and releasing knots in cord, thread or ribbon." },
  { id: "lunar-practices", label: "Lunar Practices", description: "Practices timed to or structured around the moon's phases." },
  { id: "charms-talismans", label: "Charms & Talismans", description: "Making, carrying or placing a crafted object that holds an intention." },
  { id: "crystals-stones", label: "Crystals & Stones", description: "Working with stones, crystals and minerals that are safe to handle." },
  { id: "divination", label: "Divination", description: "Reading tarot, runes, pendulums, scrying surfaces or natural signs." },
  { id: "altars-sacred-space", label: "Altars & Sacred Space", description: "Building altars, casting circles, and preparing or closing ritual space." },
  { id: "elemental-practices", label: "Elemental Practices", description: "Practices organized around earth, air, fire and water." },
  { id: "potions-infusions", label: "Potions & Infusions", description: "Ritual infusions, baths, oils, sprays and jars, always with the potion safety disclosure." },
] as const;

export type LessonCategoryId = (typeof LESSON_CATEGORIES)[number]["id"];
export const LESSON_CATEGORY_IDS: readonly LessonCategoryId[] = LESSON_CATEGORIES.map((category) => category.id);

export function isLessonCategoryId(value: unknown): value is LessonCategoryId {
  return typeof value === "string" && (LESSON_CATEGORY_IDS as readonly string[]).includes(value);
}

/** Earlier practical category IDs (practicalLesson.ts) mapped to the current method categories. */
export const LEGACY_PRACTICAL_CATEGORY_MAP: Readonly<Record<string, LessonCategoryId>> = {
  "candle-magic": "candle-spells",
  "herbs-botanicals": "herbal-rituals",
  "potions-infusions": "potions-infusions",
  "crystals-stones": "crystals-stones",
  "cord-knot-magic": "cord-knot-work",
  "moon-phases-timing": "lunar-practices",
  "elemental-magic": "elemental-practices",
  "altars-sacred-space": "altars-sacred-space",
  "divination": "divination",
};

/**
 * Outcome-based legacy categories are not methods. They become tags, and the
 * method is inferred from the lesson itself (see inferCategoryFromText); the
 * fallback below is used only when nothing in the text names a method.
 */
export const LEGACY_INTENTION_CATEGORIES: Readonly<Record<string, { tag: string; fallback: LessonCategoryId }>> = {
  "Protection": { tag: "protection", fallback: "charms-talismans" },
  "LoveAndRelationships": { tag: "relationships", fallback: "candle-spells" },
  "ProsperityAndAbundance": { tag: "abundance", fallback: "charms-talismans" },
  "HealingAndWellness": { tag: "wellbeing", fallback: "herbal-rituals" },
  "DivinationAndClarity": { tag: "clarity", fallback: "divination" },
  "BanishingAndRelease": { tag: "release", fallback: "cord-knot-work" },
  "HomeAndHearth": { tag: "home", fallback: "altars-sacred-space" },
  "MoonAndSeasonal": { tag: "seasonal", fallback: "lunar-practices" },
  "protection-cleansing-warding": { tag: "protection", fallback: "charms-talismans" },
};

const METHOD_KEYWORDS: ReadonlyArray<[LessonCategoryId, RegExp]> = [
  ["potions-infusions", /\b(potion|infusion|tea|bath|oil|spray|jar|tincture|brew)s?\b/],
  ["candle-spells", /\b(candle|taper|votive|wick|flame)s?\b/],
  ["cord-knot-work", /\b(cord|knot|thread|string|ribbon|braid)s?\b/],
  ["divination", /\b(tarot|rune|pendulum|scry|scrying|omen|divination|augury)s?\b/],
  ["crystals-stones", /\b(crystal|stone|quartz|amethyst|pebble|mineral)s?\b/],
  ["lunar-practices", /\b(moon|lunar|crescent|waxing|waning)\b/],
  ["herbal-rituals", /\b(herb|herbs|sachet|bundle|rosemary|lavender|sage|botanical|leaf|leaves|petal)s?\b/],
  ["charms-talismans", /\b(charm|talisman|amulet|token|sigil)s?\b/],
  ["altars-sacred-space", /\b(altar|circle|threshold|shrine|sacred space)s?\b/],
  ["elemental-practices", /\b(element|elemental|earth|air|water|fire)s?\b/],
];

/** The first method the lesson text actually names, in priority order; null when none. */
export function inferCategoryFromText(text: string): LessonCategoryId | null {
  const lower = text.toLowerCase();
  for (const [category, pattern] of METHOD_KEYWORDS) {
    if (pattern.test(lower)) return category;
  }
  return null;
}
