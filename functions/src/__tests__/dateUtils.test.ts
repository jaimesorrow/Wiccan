import { getUserLocalDate } from "../dateUtils";

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
});
