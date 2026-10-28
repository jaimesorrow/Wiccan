import { MAX_TIME_ZONE_LENGTH } from "../callableInput";
import { getTodayContent } from "../index";
import { FirestoreContentRepository } from "../firestoreContentRepository";
import { FirestoreSystemConfigRepository } from "../firestoreSystemConfigRepository";

// Calling .run bypasses SDK token verification; no Auth/App Check service is needed.
jest.mock("firebase-admin/auth", () => ({ getAuth: jest.fn() }));
jest.mock("firebase-admin/app-check", () => ({ getAppCheck: jest.fn() }));

function request(
  timezone: unknown,
  emailVerified = true,
): Parameters<typeof getTodayContent.run>[0] {
  return {
    auth: { uid: "timezone-test-user", token: { email_verified: emailVerified } },
    data: { timezone },
  } as
    Parameters<typeof getTodayContent.run>[0];
}

describe("getTodayContent callable time-zone validation", () => {
  afterEach(() => jest.restoreAllMocks());

  it("rejects an oversized time zone before Intl parsing", async () => {
    const format = jest.spyOn(Intl, "DateTimeFormat");

    await expect(getTodayContent.run(request("x".repeat(MAX_TIME_ZONE_LENGTH + 1))))
      .rejects.toMatchObject({ code: "invalid-argument" });
    expect(format).not.toHaveBeenCalled();
  });

  it.each([null, 7, "", " UTC", "UTC ", "Not/A_Real_Zone"])(
    "rejects an invalid supplied time zone: %p",
    async (timezone) => {
      await expect(getTodayContent.run(request(timezone)))
        .rejects.toMatchObject({ code: "invalid-argument" });
    },
  );

  it("still requires authentication before validating the payload", async () => {
    const unauthenticated = { data: { timezone: "UTC" } } as
      Parameters<typeof getTodayContent.run>[0];
    await expect(getTodayContent.run(unauthenticated))
      .rejects.toMatchObject({ code: "unauthenticated" });
  });

  it("requires a verified email before validating the payload", async () => {
    await expect(getTodayContent.run(request("Not/A_Real_Zone", false)))
      .rejects.toMatchObject({ code: "permission-denied" });
  });

  it("returns permission-denied with a subscription reason before reading assigned content", async () => {
    jest.spyOn(FirestoreSystemConfigRepository.prototype, "getConfig")
      .mockResolvedValue({ dailyContentEnabled: true, notificationsEnabled: true });
    jest.spyOn(FirestoreContentRepository.prototype, "getUserTier").mockResolvedValue("free");
    const history = jest.spyOn(FirestoreContentRepository.prototype, "findTodayDeliveredContentId");
    await expect(getTodayContent.run(request("UTC")))
      .rejects.toMatchObject({ code: "permission-denied", details: { reason: "SUBSCRIPTION_REQUIRED" } });
    expect(history).not.toHaveBeenCalled();
  });
});
