/** yyyy-MM-dd in UTC, used as the day boundary for delivery history. */
export function todayIsoDate(now: Date = new Date()): string {
  return now.toISOString().slice(0, 10);
}
