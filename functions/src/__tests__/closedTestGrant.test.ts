import { closedTestGrant, parseClosedTestEnd } from "../closedTestGrant";

describe("closedTestGrant", () => {
  const now = Date.parse("2026-11-24T00:00:00.000Z");

  it("writes premium access that expires and never a purchase token", () => {
    const grant = closedTestGrant(now + 86_400_000, now);
    expect(grant).toEqual({
      tier: "premium",
      subscriptionExpiresAtMillis: now + 86_400_000,
      closedTestGrant: true,
      closedTestGrantedAtMillis: now,
    });
    expect(grant).not.toHaveProperty("purchaseToken");
  });

  it.each([now, now - 1, Number.NaN, Number.POSITIVE_INFINITY])("rejects an unusable end %p", (ends) => {
    expect(() => closedTestGrant(ends, now)).toThrow(/future/);
  });

  it("parses a calendar end as the last millisecond of that UTC day", () => {
    expect(parseClosedTestEnd("2026-12-31", now)).toBe(Date.parse("2026-12-31T23:59:59.999Z"));
  });
});
