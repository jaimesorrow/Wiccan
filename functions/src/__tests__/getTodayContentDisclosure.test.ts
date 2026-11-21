import { getTodayContent } from "../index";
import { FirestoreContentRepository } from "../firestoreContentRepository";
import { FirestoreSystemConfigRepository } from "../firestoreSystemConfigRepository";
import { FirestoreUserProfileRepository } from "../firestoreUserProfileRepository";
import { CURRENT_PRIVACY_VERSION, CURRENT_TERMS_VERSION } from "../policyVersions";
import { makeContent } from "../testUtils/fakeContentRepository";
import { getFirestore } from "firebase-admin/firestore";
import * as logger from "firebase-functions/logger";

// Calling .run bypasses SDK token verification; no Auth/App Check service is needed.
jest.mock("firebase-admin/auth", () => ({ getAuth: jest.fn() }));
jest.mock("firebase-admin/app-check", () => ({ getAppCheck: jest.fn() }));
jest.mock("firebase-admin/firestore", () => {
  const actual = jest.requireActual("firebase-admin/firestore");
  return { ...actual, getFirestore: jest.fn() };
});
jest.mock("firebase-functions/logger", () => ({
  error: jest.fn(),
  info: jest.fn(),
  warn: jest.fn(),
  debug: jest.fn(),
  log: jest.fn(),
}));

const POTION_DISCLOSURE = "Ritual use only. Do not ingest unless a qualified practitioner has approved the recipe.";
const UID = "potion-disclosure-user";

function request(): Parameters<typeof getTodayContent.run>[0] {
  return {
    auth: { uid: UID, token: { email_verified: true } },
    data: { timezone: "UTC" },
  } as Parameters<typeof getTodayContent.run>[0];
}

function potionContent() {
  return makeContent({
    id: "potion-today",
    type: "potion",
    tier: "premium",
    title: "Chamomile evening infusion",
    requiresPotionDisclosure: true,
    safetyNote: "Ritual use only.",
  });
}

function spellContent() {
  return makeContent({
    id: "spell-today",
    type: "spell",
    tier: "premium",
    title: "A simple ward",
    requiresPotionDisclosure: false,
  });
}

function mockDailyContentEnabled() {
  jest.spyOn(FirestoreSystemConfigRepository.prototype, "getConfig")
    .mockResolvedValue({
      dailyContentEnabled: true,
      notificationsEnabled: true,
      contentGenerationEnabled: false,
      contentGenerationBatchSize: 3,
    });
  jest.spyOn(FirestoreUserProfileRepository.prototype, "getAcceptedPolicyVersions")
    .mockResolvedValue({ termsVersion: CURRENT_TERMS_VERSION, privacyVersion: CURRENT_PRIVACY_VERSION });
}

function mockAssignedContent(content: ReturnType<typeof potionContent>) {
  jest.spyOn(FirestoreContentRepository.prototype, "getUserTier").mockResolvedValue("premium");
  jest.spyOn(FirestoreContentRepository.prototype, "findTodayDeliveredContentId").mockResolvedValue(content.id);
  jest.spyOn(FirestoreContentRepository.prototype, "getContentById").mockResolvedValue(content);
}

function mockDisclosures(configured: unknown) {
  const get = jest.fn().mockImplementation((field: string) => (
    field === "potionSafetyDisclosure" ? configured : undefined
  ));
  (getFirestore as jest.Mock).mockReturnValue({
    collection: (name: string) => ({
      doc: (id: string) => ({
        get: async () => {
          if (name !== "settings" || id !== "disclosures") {
            throw new Error(`Unexpected Firestore read: ${name}/${id}`);
          }
          return { exists: configured !== undefined, get };
        },
      }),
    }),
  });
  return get;
}

describe("getTodayContent potion safety disclosure", () => {
  afterEach(() => jest.restoreAllMocks());

  beforeEach(() => {
    // Isolate this file from the real Admin SDK: constructors still receive a
    // Firestore-like object, but repository methods are stubbed and only
    // settings/disclosures is readable through the stub.
    (getFirestore as jest.Mock).mockReturnValue({
      collection: (name: string) => ({
        doc: (id: string) => ({
          get: async () => {
            throw new Error(`Unexpected Firestore read: ${name}/${id}`);
          },
        }),
      }),
    });
  });

  it("returns the configured disclosure on a potion day", async () => {
    mockDailyContentEnabled();
    const content = potionContent();
    mockAssignedContent(content);
    mockDisclosures(POTION_DISCLOSURE);

    const result = await getTodayContent.run(request());

    expect(result).toEqual(expect.objectContaining({
      id: content.id,
      type: "potion",
      title: content.title,
      requiresPotionDisclosure: true,
      potionSafetyDisclosure: POTION_DISCLOSURE,
      timezone: "UTC",
    }));
    expect(result.deliveredDate).toMatch(/^\d{4}-\d{2}-\d{2}$/);
    expect(getFirestore).toHaveBeenCalled();
  });

  it("does not read or attach a disclosure when the assigned content does not require one", async () => {
    mockDailyContentEnabled();
    mockAssignedContent(spellContent());
    const get = mockDisclosures(POTION_DISCLOSURE);

    const result = await getTodayContent.run(request());

    expect(result.potionSafetyDisclosure).toBeUndefined();
    expect(result).toEqual(expect.objectContaining({ id: "spell-today", type: "spell" }));
    expect(get).not.toHaveBeenCalled();
  });

  it.each([
    { label: "the disclosures document is missing", configured: undefined },
    { label: "potionSafetyDisclosure is missing", configured: undefined, existsOnly: true },
    { label: "potionSafetyDisclosure is empty", configured: "" },
    { label: "potionSafetyDisclosure is whitespace", configured: "   " },
  ])("fails with the existing internal error when $label", async ({ configured, existsOnly }) => {
    mockDailyContentEnabled();
    mockAssignedContent(potionContent());
    const get = jest.fn().mockImplementation((field: string) => (
      field === "potionSafetyDisclosure" ? configured : undefined
    ));
    (getFirestore as jest.Mock).mockReturnValue({
      collection: (name: string) => ({
        doc: (id: string) => ({
          get: async () => {
            if (name !== "settings" || id !== "disclosures") {
              throw new Error(`Unexpected Firestore read: ${name}/${id}`);
            }
            return { exists: existsOnly === true || configured !== undefined, get };
          },
        }),
      }),
    });
    await expect(getTodayContent.run(request()))
      .rejects.toMatchObject({ code: "internal", message: "Could not select today's content." });
    expect(logger.error).toHaveBeenCalledWith("getTodayContent failed", {
      uid: UID,
      error: "Required potionSafetyDisclosure is not configured.",
    });
  });
});
