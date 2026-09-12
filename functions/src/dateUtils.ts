/**
 * The single source of truth for "what date is it for this user right
 * now", used everywhere a delivery/history date is computed (daily
 * content assignment, history document IDs, scheduled notifications).
 * Never compute a delivery date independently elsewhere - two call sites
 * disagreeing on "today" for the same user is exactly the inconsistency
 * this function exists to prevent.
 *
 * Falls back to UTC when `timeZone` is missing or not a valid IANA
 * identifier, so a user who hasn't had a device timezone recorded yet
 * still gets a well-defined, stable "today" rather than an error.
 */
export function getUserLocalDate(timeZone: string | null | undefined, now: Date = new Date()): string {
  const zone = timeZone && isValidTimeZone(timeZone) ? timeZone : "UTC";
  // en-CA's date format is yyyy-MM-dd, exactly the ISO date shape this
  // codebase stores everywhere - no manual reformatting needed.
  return new Intl.DateTimeFormat("en-CA", {
    timeZone: zone,
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).format(now);
}

/**
 * The current hour (0-23) in the given time zone - the other half of "what
 * time is it for this user right now", alongside getUserLocalDate. Used to
 * decide whether a device's local time currently falls in its 9am delivery
 * window. Same UTC/invalid-zone fallback as getUserLocalDate.
 */
export function getUserLocalHour(timeZone: string | null | undefined, now: Date = new Date()): number {
  const zone = timeZone && isValidTimeZone(timeZone) ? timeZone : "UTC";
  const hourString = new Intl.DateTimeFormat("en-US", {
    timeZone: zone,
    hour: "numeric",
    hour12: false,
  }).format(now);
  // Some locales/runtimes render midnight as "24" rather than "00" under
  // hour12: false - normalize so callers always get a plain 0-23 value.
  return Number(hourString) % 24;
}

function isValidTimeZone(timeZone: string): boolean {
  try {
    new Intl.DateTimeFormat(undefined, { timeZone });
    return true;
  } catch {
    return false;
  }
}
