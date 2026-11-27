import { moonPhase, sabbatFor, seasonalWindowFor } from "../seasonalCalendar";
import { getUserLocalDate } from "../dateUtils";

describe("seasonal calendar", () => {
  it.each([
    ["2026-01-18", "new"],
    ["2026-03-03", "full"],
    ["2026-08-12", "new"],
    ["2027-01-22", "full"],
  ] as const)("classifies %s as %s", (date, phase) => {
    expect(moonPhase(date)).toBe(phase);
  });

  it.each([
    ["2026-02-01", "imbolc"], ["2026-03-20", "ostara"],
    ["2026-05-01", "beltane"], ["2026-06-21", "litha"],
    ["2026-08-01", "lughnasadh"], ["2026-09-22", "mabon"],
    ["2026-10-31", "samhain"], ["2026-12-21", "yule"],
  ] as const)("maps %s to %s", (date, sabbat) => {
    expect(sabbatFor(date)).toBe(sabbat);
  });

  it("returns no event for an ordinary day and rejects impossible dates", () => {
    expect(seasonalWindowFor("2026-07-07")).toBeNull();
    expect(() => moonPhase("2026-02-30")).toThrow("Invalid calendar date");
  });

  it.each([
    ["2026-01-16", null],
    ["2026-01-17", { kind: "moon", phase: "new" }],
    ["2026-01-19", { kind: "moon", phase: "new" }],
    ["2026-01-20", null],
    ["2026-03-01", null],
    ["2026-03-02", { kind: "moon", phase: "full" }],
    ["2026-03-04", { kind: "moon", phase: "full" }],
    ["2026-03-05", null],
    ["2026-03-16", null],
    ["2026-03-17", { kind: "moon", phase: "new" }],
    ["2026-03-19", { kind: "moon", phase: "new" }],
    ["2026-03-20", { kind: "sabbat", id: "ostara" }],
  ])("returns the same civil-date event as the chip on %s", (date, event) => {
    expect(seasonalWindowFor(date)).toEqual(event);
  });

  it.each([
    ["2026-03-02T00:30:00Z", "America/Los_Angeles", "2026-03-01", null],
    ["2026-03-02T00:30:00Z", "UTC", "2026-03-02", { kind: "moon", phase: "full" }],
    ["2026-03-01T12:30:00Z", "Pacific/Kiritimati", "2026-03-02", { kind: "moon", phase: "full" }],
  ])("uses the same local date and event as the chip at %s in %s", (instant, zone, expectedDate, event) => {
    const date = getUserLocalDate(zone, new Date(instant));
    expect(date).toBe(expectedDate);
    expect(seasonalWindowFor(date)).toEqual(event);
  });
});
