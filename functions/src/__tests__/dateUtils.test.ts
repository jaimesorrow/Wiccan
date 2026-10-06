import { getUserLocalDate, getUserLocalHour, isValidTimeZone, resolveTimeZone } from "../dateUtils";

describe("getUserLocalDate", () => {
  it("falls back to UTC when time zone is null", () => {
    const now = new Date("2026-09-10T12:00:00.000Z");
    expect(getUserLocalDate(null, now)).toBe("2026-09-10");
  });

  it("falls back to UTC when time zone is an invalid IANA identifier", () => {
    const now = new Date("2026-09-10T12:00:00.000Z");
    expect(getUserLocalDate("Not/A_Real_Zone", now)).toBe("2026-09-10");
  });

  it("returns the local calendar date, which can differ from UTC across a day boundary", () => {
    // 23:30 UTC on Sep 10 is already past midnight (00:30) in a UTC+13 zone.
    const now = new Date("2026-09-10T23:30:00.000Z");
    expect(getUserLocalDate("UTC", now)).toBe("2026-09-10");
    expect(getUserLocalDate("Pacific/Auckland", now)).toBe("2026-09-11");
  });

  it("returns yesterday's date in a zone far enough behind UTC", () => {
    // 00:30 UTC on Sep 10 is still 13:30 the previous day in UTC-11.
    const now = new Date("2026-09-10T00:30:00.000Z");
    expect(getUserLocalDate("Pacific/Midway", now)).toBe("2026-09-09");
  });

  it("handles a DST transition correctly (America/New_York spring-forward day)", () => {
    // 2026-03-08 is the US DST transition; a moment that is Mar 8 in UTC
    // must still resolve to the correct local calendar date either side.
    const beforeTransition = new Date("2026-03-08T04:00:00.000Z"); // 23:00 EST Mar 7
    const afterTransition = new Date("2026-03-08T12:00:00.000Z"); // 08:00 EDT Mar 8
    expect(getUserLocalDate("America/New_York", beforeTransition)).toBe("2026-03-07");
    expect(getUserLocalDate("America/New_York", afterTransition)).toBe("2026-03-08");
  });

  it("preserves Android fixed-offset calendar dates including fractional offsets", () => {
    expect(getUserLocalDate("GMT+05:30", new Date("2026-10-06T19:00:00Z"))).toBe("2026-10-07");
    expect(getUserLocalDate("GMT-03:30", new Date("2026-10-06T02:00:00Z"))).toBe("2026-10-05");
    expect(resolveTimeZone("GMT+05:30")).toBe("+05:30");
    expect(isValidTimeZone("GMT+25:30")).toBe(false);
  });
});

describe("getUserLocalHour", () => {
  it("falls back to UTC when time zone is null or invalid", () => {
    const now = new Date("2026-01-01T09:00:00.000Z");
    expect(getUserLocalHour(null, now)).toBe(9);
    expect(getUserLocalHour("Not/A_Real_Zone", now)).toBe(9);
  });

  it("computes the local hour for a positive UTC offset", () => {
    // 09:00 UTC is 18:00 in UTC+9 (Asia/Tokyo).
    const now = new Date("2026-01-01T09:00:00.000Z");
    expect(getUserLocalHour("Asia/Tokyo", now)).toBe(18);
  });

  it("computes the local hour for a negative UTC offset, wrapping to the previous day", () => {
    // 02:00 UTC is 15:00 the previous day in UTC-11.
    const now = new Date("2026-01-01T02:00:00.000Z");
    expect(getUserLocalHour("Pacific/Midway", now)).toBe(15);
  });

  it("normalizes midnight to 0 rather than 24", () => {
    const now = new Date("2026-01-01T00:00:00.000Z");
    expect(getUserLocalHour("UTC", now)).toBe(0);
  });

  it("uses the same normalized Android offset as daily assignment", () => {
    expect(getUserLocalHour("GMT+05:30", new Date("2026-10-06T03:30:00Z"))).toBe(9);
  });
});
