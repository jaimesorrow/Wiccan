import type { MoonPhaseId, SabbatId, SeasonalWindow } from "./types";

const SYNODIC_MONTH_DAYS = 29.530588853;
const KNOWN_NEW_MOON_MS = Date.UTC(2000, 0, 6, 18, 14);
const DAY_MS = 86_400_000;

const SABBATS: Readonly<Record<string, SabbatId>> = {
  "02-01": "imbolc",
  "03-20": "ostara",
  "05-01": "beltane",
  "06-21": "litha",
  "08-01": "lughnasadh",
  "09-22": "mabon",
  "10-31": "samhain",
  "12-21": "yule",
};

function utcNoon(date: string): number {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(date)) throw new Error(`Invalid calendar date '${date}'.`);
  const value = Date.parse(`${date}T12:00:00.000Z`);
  if (!Number.isFinite(value) || new Date(value).toISOString().slice(0, 10) !== date) {
    throw new Error(`Invalid calendar date '${date}'.`);
  }
  return value;
}

/** Approximate lunar phase for a civil date, accurate enough for a daily experience. */
export function moonPhase(date: string): MoonPhaseId {
  const cycles = (utcNoon(date) - KNOWN_NEW_MOON_MS) / DAY_MS / SYNODIC_MONTH_DAYS;
  const age = ((cycles % 1) + 1) % 1 * SYNODIC_MONTH_DAYS;
  if (age < 1.5 || age > SYNODIC_MONTH_DAYS - 1.5) return "new";
  if (Math.abs(age - SYNODIC_MONTH_DAYS / 2) < 1.5) return "full";
  return age < SYNODIC_MONTH_DAYS / 2 ? "waxing" : "waning";
}

/** Product-calendar sabbat anchor for the supplied date; it makes no historical claim. */
export function sabbatFor(date: string): SabbatId | null {
  utcNoon(date);
  return SABBATS[date.slice(5)] ?? null;
}

export function seasonalWindowFor(date: string): SeasonalWindow | null {
  const sabbat = sabbatFor(date);
  if (sabbat) return { kind: "sabbat", id: sabbat };
  const phase = moonPhase(date);
  return phase === "new" || phase === "full" ? { kind: "moon", phase } : null;
}

export function matchesSeasonalWindow(window: SeasonalWindow | undefined, today: SeasonalWindow | null): boolean {
  if (!window || !today || window.kind !== today.kind) return false;
  return window.kind === "sabbat"
    ? window.id === (today as Extract<SeasonalWindow, { kind: "sabbat" }>).id
    : window.phase === (today as Extract<SeasonalWindow, { kind: "moon" }>).phase;
}
