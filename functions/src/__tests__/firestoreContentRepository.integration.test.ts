import { App, deleteApp, initializeApp } from "firebase-admin/app";
import { Firestore, getFirestore } from "firebase-admin/firestore";
import type { Auth } from "firebase-admin/auth";
import { FirestoreContentRepository } from "../firestoreContentRepository";
import {
  CONTENT_GENERATION_SOURCE_KIND,
  CONTENT_GENERATOR_ID,
  FirestoreContentAdminRepository,
} from "../firestoreContentAdminRepository";
import { FirestoreAccountDeletionRepository } from "../firestoreAccountDeletionRepository";
import { ContentAlreadyDeliveredError, ContentNoLongerEligibleError, SubscriptionRequiredError } from "../contentRepositoryPort";
import { ContentPoolExhaustedError, DailyAssignmentUnavailableError, selectTodayContent } from "../getTodayContent";
import { makeContent } from "../testUtils/fakeContentRepository";
import { FakePlayBillingPort } from "../testUtils/fakePlayBillingPort";
import { Content } from "../types";
import { getUserLocalDate } from "../dateUtils";

// The emulator suite exercises the production repository and real Firestore
// transactions. Ordinary unit-test runs do not contact a live project.
const describeWithEmulator = process.env.FIRESTORE_EMULATOR_HOST ? describe : describe.skip;
const UID = "daily-integration-user";
const TODAY = "2026-10-06";
const ANCIENT_SOURCES = new Set(["ancient-source"]);

// Real Firestore transaction retries use backoff under contention. A cold CI
// emulator can exceed Jest's five-second unit-test budget before they finish.
jest.setTimeout(15_000);

describeWithEmulator("Firestore daily assignment transactions", () => {
  let app: App;
  let db: Firestore;
  let repo: FirestoreContentRepository;

  beforeAll(() => {
    app = initializeApp({ projectId: "demo-wiccan" }, "daily-assignment-tests");
    db = getFirestore(app);
    repo = new FirestoreContentRepository(db);
  });

  beforeEach(async () => {
    for (const name of ["content", "users", "userContentHistory", "userContentUsage", "contentPoolExhaustionEvents"]) {
      const documents = await db.collection(name).listDocuments();
      await Promise.all(documents.map((document) => db.recursiveDelete(document)));
    }
    await db.collection("users").doc(UID).set({ tier: "premium", subscriptionExpiresAtMillis: Date.now() + 3_600_000, timeZone: "America/Denver" });
  });

  afterAll(async () => {
    await db.terminate();
    await deleteApp(app);
  });

  async function seed(...items: Content[]): Promise<void> {
    await Promise.all(items.map(({ id, ...content }) => db.collection("content").doc(id).set(content)));
  }

  async function seedLegacyHistory(contentId: string, overrides: Record<string, unknown> = {}): Promise<void> {
    await db.collection("userContentHistory").doc(`${UID}_2026-10-05`).set({
      userId: UID, contentId, deliveredDate: "2026-10-05", title: "Historical title",
      type: "lesson", tradition: "Ancient Egyptian religion", category: "Protection", ...overrides,
    });
  }

  it("concurrent same-day requests produce one history row and one authoritative winner", async () => {
    await seed(makeContent({ id: "a" }), makeContent({ id: "b" }), makeContent({ id: "c" }));
    const results = await Promise.all(Array.from({ length: 3 }, () => selectTodayContent(UID, TODAY, repo)));

    expect(new Set(results.map((content) => content.id)).size).toBe(1);
    const history = await db.collection("userContentHistory").where("userId", "==", UID).get();
    expect(history.docs).toHaveLength(1);
    expect(history.docs[0].id).toBe(`${UID}_${TODAY}`);
    expect(history.docs[0].get("contentId")).toBe(results[0].id);
    expect((await db.collection("userContentUsage").doc(UID).collection("content").get()).size).toBe(1);
    expect((await selectTodayContent(UID, TODAY, repo)).id).toBe(results[0].id);
  }, 15_000);

  it("legacy delivery history prevents a repeated ID even without a usage marker", async () => {
    const content = makeContent({ id: "seen" });
    await seed(content);
    await seedLegacyHistory(content.id);

    await expect(repo.assignDailyContent(UID, TODAY, content, ANCIENT_SOURCES))
      .rejects.toBeInstanceOf(ContentAlreadyDeliveredError);
    expect((await db.collection("userContentHistory").doc(`${UID}_${TODAY}`).get()).exists).toBe(false);
  });

  it("legacy delivery history excludes a new editorial rewording", async () => {
    const original = makeContent({ id: "original", similarityGroupId: "shared-concept" });
    const reworded = makeContent({ id: "reworded", similarityGroupId: "shared-concept" });
    await seed(original, reworded, makeContent({ id: "unseen", similarityGroupId: "another-concept" }));
    await seedLegacyHistory(original.id);

    await expect(repo.assignDailyContent(UID, TODAY, reworded, ANCIENT_SOURCES))
      .rejects.toBeInstanceOf(ContentAlreadyDeliveredError);
    expect((await selectTodayContent(UID, TODAY, repo)).id).toBe("unseen");
    expect((await db.collection("userContentHistory").get()).size).toBe(2);
  });

  it("retains denormalized similarity protection when historical content was removed", async () => {
    const reworded = makeContent({ id: "reworded", similarityGroupId: "shared-concept" });
    await seed(reworded);
    await seedLegacyHistory("removed", { similarityGroupId: "shared-concept" });
    await expect(selectTodayContent(UID, TODAY, repo)).rejects.toBeInstanceOf(ContentPoolExhaustedError);
  });

  it("retains passage provenance when a historical document has been removed", async () => {
    const reworded = makeContent({ id: "reworded", similarityGroupId: "new-editorial-group", sourceId: "museum-source", sourceCitation: "Passage 1" });
    await seed(reworded);
    await seedLegacyHistory("removed", { sourceId: "museum-source", sourceCitation: "passage 1" });
    await expect(selectTodayContent(UID, TODAY, repo)).rejects.toBeInstanceOf(ContentPoolExhaustedError);
  });

  it("identifies a legacy rewording from its source passage without overgrouping explicit concepts", async () => {
    const original = makeContent({ id: "original", sourceId: "museum", sourceCitation: "Passage 1" });
    const reworded = makeContent({ id: "reworded", sourceId: "museum", sourceCitation: " Passage   1 " });
    await seed(original, reworded);
    await seedLegacyHistory(original.id);
    await expect(repo.assignDailyContent(UID, TODAY, reworded, ANCIENT_SOURCES))
      .rejects.toBeInstanceOf(ContentAlreadyDeliveredError);

    await seed({ ...reworded, similarityGroupId: "newly-annotated" });
    await expect(repo.assignDailyContent(UID, TODAY, reworded, ANCIENT_SOURCES))
      .rejects.toBeInstanceOf(ContentAlreadyDeliveredError);

    await seed({ ...original, similarityGroupId: "eye" }, { ...reworded, similarityGroupId: "heart" });
    expect(await repo.assignDailyContent(UID, TODAY, reworded, ANCIENT_SOURCES)).toBe(reworded.id);
  });

  it("uses the same local-date history key across a UTC midnight boundary", async () => {
    await seed(makeContent({ id: "a" }), makeContent({ id: "b" }));
    const zone = await repo.getUserTimeZone(UID);
    const earlier = getUserLocalDate(zone, new Date("2026-10-06T23:30:00Z"));
    const later = getUserLocalDate(zone, new Date("2026-10-07T00:30:00Z"));
    expect(earlier).toBe(TODAY);
    expect(later).toBe(TODAY);
    expect((await selectTodayContent(UID, earlier, repo)).id).toBe((await selectTodayContent(UID, later, repo)).id);
    expect((await db.collection("userContentHistory").get()).size).toBe(1);
  });

  it("atomically reserves a similarity group across concurrent different dates", async () => {
    const first = makeContent({ id: "a", similarityGroupId: "shared/concept" });
    const second = makeContent({ id: "b", similarityGroupId: "shared/concept" });
    await seed(first, second);
    const outcomes = await Promise.allSettled([
      repo.assignDailyContent(UID, TODAY, first, ANCIENT_SOURCES),
      repo.assignDailyContent(UID, "2026-10-07", second, ANCIENT_SOURCES),
    ]);

    expect(outcomes.filter((result) => result.status === "fulfilled")).toHaveLength(1);
    expect(outcomes.filter((result) => result.status === "rejected")).toHaveLength(1);
    const rejected = outcomes.find((result) => result.status === "rejected") as PromiseRejectedResult;
    expect(rejected.reason).toBeInstanceOf(ContentAlreadyDeliveredError);
    expect((await db.collection("userContentHistory").get()).size).toBe(1);
    expect((await db.collection("userContentUsage").doc(UID).collection("similarityGroups").get()).size).toBe(1);
  });

  it("serializes concurrent rewordings with mixed legacy passage and explicit group metadata", async () => {
    const legacy = makeContent({ id: "legacy", sourceId: "museum", sourceCitation: "Passage 1" });
    const modern = makeContent({ id: "modern", sourceId: "museum", sourceCitation: "Passage 1", similarityGroupId: "explicit-group" });
    await seed(legacy, modern);
    const outcomes = await Promise.allSettled([
      repo.assignDailyContent(UID, TODAY, legacy, ANCIENT_SOURCES),
      repo.assignDailyContent(UID, "2026-10-07", modern, ANCIENT_SOURCES),
    ]);
    expect(outcomes.filter((result) => result.status === "fulfilled")).toHaveLength(1);
    expect(outcomes.filter((result) => result.status === "rejected")).toHaveLength(1);
    expect((await db.collection("userContentHistory").get()).size).toBe(1);
    expect((await db.collection("userContentUsage").doc(UID).get()).exists).toBe(true);
  });

  it.each(["legacy", "modern"])("excludes mixed-metadata rewordings when %s is delivered first", async (firstId) => {
    const legacy = makeContent({ id: "legacy", sourceId: "museum", sourceCitation: "Passage 1" });
    const modern = makeContent({ id: "modern", sourceId: "museum", sourceCitation: "Passage 1", similarityGroupId: "explicit-group" });
    await seed(legacy, modern);
    const first = firstId === "legacy" ? legacy : modern;
    const second = firstId === "legacy" ? modern : legacy;
    expect(await repo.assignDailyContent(UID, TODAY, first, ANCIENT_SOURCES)).toBe(first.id);
    await expect(repo.assignDailyContent(UID, "2026-10-07", second, ANCIENT_SOURCES))
      .rejects.toBeInstanceOf(ContentAlreadyDeliveredError);
    expect((await db.collection("userContentHistory").get()).size).toBe(1);
  });

  it("different local dates receive different unseen lessons and pool exhaustion never recycles", async () => {
    await seed(makeContent({ id: "a" }), makeContent({ id: "b" }));
    const [first, second] = await Promise.all([
      selectTodayContent(UID, TODAY, repo), selectTodayContent(UID, "2026-10-07", repo),
    ]);
    expect(first.id).not.toBe(second.id);
    await expect(selectTodayContent(UID, "2026-10-08", repo)).rejects.toBeInstanceOf(ContentPoolExhaustedError);
    expect((await db.collection("userContentHistory").get()).size).toBe(2);
    expect((await db.collection("contentPoolExhaustionEvents").get()).size).toBe(1);
  }, 15_000);

  it("uses one pool for subscribers and denies all content after expiry, including today's assignment", async () => {
    const free = makeContent({ id: "free" });
    const premium = makeContent({ id: "premium", tier: "premium" });
    await seed(free, premium);
    expect(await repo.getPublishedContentForTier("free")).toEqual([]);
    expect(new Set((await repo.getPublishedContentForTier("premium")).map((content) => content.id)))
      .toEqual(new Set([free.id, premium.id]));
    const first = await selectTodayContent(UID, TODAY, repo);
    await db.collection("users").doc(UID).update({ subscriptionExpiresAtMillis: Date.now() - 1 });
    expect(await repo.getUserTier(UID)).toBe("free");
    await expect(selectTodayContent(UID, TODAY, repo)).rejects.toBeInstanceOf(SubscriptionRequiredError);
    await expect(repo.assignDailyContent(UID, TODAY, free, ANCIENT_SOURCES)).rejects.toBeInstanceOf(SubscriptionRequiredError);
    await db.collection("users").doc(UID).update({ subscriptionExpiresAtMillis: Date.now() + 3_600_000 });
    expect((await selectTodayContent(UID, TODAY, repo)).id).toBe(first.id);
    expect((await selectTodayContent(UID, "2026-10-07", repo)).id).not.toBe(first.id);
  });

  it("revalidates publication and entitlement inside the transaction", async () => {
    const candidate = makeContent({ id: "stale", tier: "free" });
    await seed({ ...candidate, status: "draft" });
    await expect(repo.assignDailyContent(UID, TODAY, candidate, ANCIENT_SOURCES))
      .rejects.toBeInstanceOf(ContentNoLongerEligibleError);

    await seed({ ...candidate, tier: "premium" });
    await db.collection("users").doc(UID).update({ tier: "free" });
    await expect(repo.assignDailyContent(UID, TODAY, candidate, ANCIENT_SOURCES))
      .rejects.toBeInstanceOf(SubscriptionRequiredError);
    await db.collection("users").doc(UID).update({ tier: "premium" });
    expect(await repo.assignDailyContent(UID, TODAY, candidate, ANCIENT_SOURCES)).toBe(candidate.id);
    expect((await db.collection("userContentHistory").get()).size).toBe(1);
  });

  it("revalidates ancient lesson provenance and writes current metadata rather than a stale snapshot", async () => {
    const candidate = makeContent({ id: "stale-lesson", type: "lesson", sourceId: "ancient-source", title: "Old title" });
    await seed({ ...candidate, sourceId: "ineligible-source" });
    await expect(repo.assignDailyContent(UID, TODAY, candidate, ANCIENT_SOURCES))
      .rejects.toBeInstanceOf(ContentNoLongerEligibleError);
    await seed({ ...candidate, title: "Edited title", similarityGroupId: "edited-group" });
    expect(await repo.assignDailyContent(UID, TODAY, candidate, ANCIENT_SOURCES)).toBe(candidate.id);
    const history = await db.collection("userContentHistory").doc(`${UID}_${TODAY}`).get();
    expect(history.get("title")).toBe("Edited title");
    expect(history.get("similarityGroupId")).toBe("edited-group");
  });

  it("preserves a missing same-day assignment instead of replacing its history", async () => {
    await seed(makeContent({ id: "unseen" }));
    await seedLegacyHistory("removed", { deliveredDate: TODAY });
    await db.collection("userContentHistory").doc(`${UID}_2026-10-05`).delete();
    await db.collection("userContentHistory").doc(`${UID}_${TODAY}`).set({
      userId: UID, contentId: "removed", deliveredDate: TODAY,
    });
    await expect(selectTodayContent(UID, TODAY, repo)).rejects.toBeInstanceOf(DailyAssignmentUnavailableError);
    expect((await db.collection("userContentHistory").doc(`${UID}_${TODAY}`).get()).get("contentId")).toBe("removed");
    expect((await db.collection("userContentHistory").get()).size).toBe(1);
  });

  it("maps all historical lesson and provenance fields while ignoring legacy difficulty", async () => {
    const item = makeContent({ id: "complete", reflection: "Reflect", practice: "Safe practice", safetyNote: "Safety", sourceId: "ancient-source", sourceCitation: "Citation", sourceUrl: "https://example.org/source", rightsNotes: "Rights", rightsReviewRequired: true });
    await seed(item);
    await db.collection("content").doc(item.id).update({ difficulty: "beginner" });
    const loaded = await repo.getContentById(item.id);
    expect(loaded).toMatchObject(item);
    expect(loaded).not.toHaveProperty("difficulty");
  });

  it("stores generated content as a review-locked draft with server audit metadata", async () => {
    const adminRepo = new FirestoreContentAdminRepository(db);
    const contentId = await adminRepo.createDraft({
      type: "lesson",
      tier: "free",
      tradition: "Ancient Egyptian religion",
      category: "Protection",
      title: "A reviewed ancient lesson",
      intro: "Historical context",
      body: "A source-grounded interpretation.",
      reflection: "What stands out?",
      practice: null,
      materials: null,
      safetyNote: null,
      tags: ["historical"],
      sourceId: "ancient-source",
      sourceCitation: "Tablet 1",
      sourceUrl: "https://example.org/source",
      rightsReviewRequired: false,
    });

    const saved = (await db.collection("content").doc(contentId).get()).data();
    expect(saved).toMatchObject({
      status: "draft",
      rightsReviewRequired: true,
      generatedBy: CONTENT_GENERATOR_ID,
      generationSourceKind: CONTENT_GENERATION_SOURCE_KIND,
    });
    expect(saved?.generatedAt).toBeDefined();
    expect(typeof saved?.generatedAt?.toDate).toBe("function");
  });

  it("authorized account deletion cleans both content and similarity reservations", async () => {
    const candidate = makeContent({ id: "grouped", similarityGroupId: "group" });
    await seed(candidate);
    await repo.assignDailyContent(UID, TODAY, candidate, ANCIENT_SOURCES);
    const deletion = new FirestoreAccountDeletionRepository(db, {} as Auth, new FakePlayBillingPort());
    await deletion.deleteUserContentUsage(UID);
    expect((await db.collection("userContentUsage").doc(UID).collection("content").get()).size).toBe(0);
    expect((await db.collection("userContentUsage").doc(UID).collection("similarityGroups").get()).size).toBe(0);
    expect((await db.collection("userContentUsage").doc(UID).get()).exists).toBe(false);
  });
});
