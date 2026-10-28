import {
  MAX_DEVICE_TOKEN_LENGTH,
  MAX_PROFILE_NAME_LENGTH,
  MAX_TIME_ZONE_LENGTH,
  parseDeviceToken,
  parseProfileName,
  parseTimeZone,
} from "../callableInput";

describe("parseDeviceToken", () => {
  it("accepts a normal FCM token", () => {
    expect(parseDeviceToken("token:with-valid_chars-123"))
      .toBe("token:with-valid_chars-123");
  });

  it.each([undefined, null, 7, "", " token", "token ", "token value"])(
    "rejects an invalid token value: %p",
    (value) => expect(parseDeviceToken(value)).toBeNull(),
  );

  it("rejects an oversized token before storage or hashing", () => {
    expect(parseDeviceToken("x".repeat(MAX_DEVICE_TOKEN_LENGTH + 1))).toBeNull();
  });
});

describe("parseTimeZone", () => {
  it.each(["UTC", "America/Anchorage", "GMT+05:30", "UTC-03:30"])(
    "accepts a supported time zone: %s",
    (value) => expect(parseTimeZone(value)).toBe(value),
  );

  it.each([undefined, null, 7, "", " UTC", "UTC ", "Not/A_Real_Zone", "GMT+25:00"])(
    "rejects an invalid time zone value: %p",
    (value) => expect(parseTimeZone(value)).toBeNull(),
  );

  it("rejects an oversized time zone before Intl parsing", () => {
    expect(parseTimeZone("x".repeat(MAX_TIME_ZONE_LENGTH + 1))).toBeNull();
  });
});

describe("parseProfileName", () => {
  it.each([
    ["Morgan", "Morgan"],
    ["  Ana Mar\u00eda  ", "Ana Mar\u00eda"],
    ["\ud83e\uddd9", "\ud83e\uddd9"],
  ])("accepts and trims a valid profile name: %p", (value, expected) => {
    expect(parseProfileName(value)).toBe(expected);
  });

  it.each([null, 7, "", "   ", "Line\nBreak", "Delete\u007fCharacter"])(
    "rejects an invalid profile name: %p",
    (value) => expect(parseProfileName(value)).toBeNull(),
  );

  it("rejects a name over the Unicode character limit", () => {
    expect(parseProfileName("\ud83e\uddd9".repeat(MAX_PROFILE_NAME_LENGTH + 1))).toBeNull();
  });
});
