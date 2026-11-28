import { App, deleteApp, initializeApp } from "firebase-admin/app";
import { Firestore, getFirestore } from "firebase-admin/firestore";
import { approveDraft, UnsafeContentError } from "../approveDraft";
import { FirestoreContentAdminRepository } from "../firestoreContentAdminRepository";
import type { NewDraftContent } from "../contentAdminRepositoryPort";

const describeWithEmulator = process.env.FIRESTORE_EMULATOR_HOST ? describe : describe.skip;

jest.setTimeout(15_000);

const APPROVAL = { operator: "test-editor", approvalDigest: "a".repeat(64) };

function draft(overrides: Partial<NewDraftContent> = {}): NewDraftContent {
  return {
    title: "Threshold Salt Line",
    intro: "A doorway closing practice.",
    body: "1. Pour a thin line of salt across the doorway.\n2. Sweep it outward and close the door.",
    materials: null,
    safetyNote: null,
    tags: ["spell"],
    category: "MoonAndSeasonal",
    type: "spell",
    tier: "free",
    tradition: "European folk magic",
    sourceId: "source-a",
    illustrationUrl: "https://cdn.example.org/originals/threshold-salt-line.png",
    illustrationOriginal: true,
    ...overrides,
  };
}

describeWithEmulator("approveDraft firestore inventory gates", () => {
  let app: App;
  let db: Firestore;
  let repo: FirestoreContentAdminRepository;

  beforeAll(() => {
    app = initializeApp({ projectId: "demo-wiccan" }, "approve-draft-gates");
    db = getFirestore(app);
    repo = new FirestoreContentAdminRepository(db);
  });

  beforeEach(async () => {
    for (const name of ["content", "contentPublicationAudit", "contentOriginalityReservations"]) {
      const documents = await db.collection(name).listDocuments();
      await Promise.all(documents.map((document) => db.recursiveDelete(document)));
    }
  });

  afterAll(async () => {
    await db.terminate();
    await deleteApp(app);
  });

  async function createReadyDraft(overrides: Partial<NewDraftContent> = {}): Promise<string> {
    const payload = Object.fromEntries(
      Object.entries(draft(overrides)).filter(([, value]) => value !== undefined),
    ) as NewDraftContent;
    const contentId = await repo.createDraft(payload);
    // Admin createDraft always flags rights review; publication requires it cleared.
    await db.collection("content").doc(contentId).update({ rightsReviewRequired: false });
    return contentId;
  }

  it("publishes a unique draft that has its own original image", async () => {
    const contentId = await createReadyDraft();
    await approveDraft(contentId, repo, APPROVAL);
    expect(await repo.getStatus(contentId)).toBe("published");
  });

  it("refuses a draft with no original image", async () => {
    const contentId = await createReadyDraft({
      illustrationUrl: undefined,
      illustrationOriginal: undefined,
    });
    await expect(approveDraft(contentId, repo, APPROVAL)).rejects.toThrow(/illustrationUrl/);
    expect(await repo.getStatus(contentId)).toBe("draft");
  });

  it("refuses a draft that reuses another item's image", async () => {
    const first = await createReadyDraft({ title: "First Spell" });
    await approveDraft(first, repo, APPROVAL);
    const second = await createReadyDraft({
      title: "Second Spell",
      body: "1. Trim a lamp wick.\n2. Light it at the gate and snuff it at night.",
    });
    await expect(approveDraft(second, repo, APPROVAL)).rejects.toThrow(UnsafeContentError);
    await expect(approveDraft(second, repo, APPROVAL)).rejects.toThrow(/already used by/);
    expect(await repo.getStatus(second)).toBe("draft");
  });

  it("refuses a draft whose normalized title already exists", async () => {
    const first = await createReadyDraft({
      title: "Threshold Salt Line",
      illustrationUrl: "https://cdn.example.org/originals/salt-a.png",
    });
    await approveDraft(first, repo, APPROVAL);
    const second = await createReadyDraft({
      title: "  threshold   SALT-line! ",
      body: "1. Trim a lamp wick.\n2. Light it at the gate and snuff it at night.",
      illustrationUrl: "https://cdn.example.org/originals/salt-b.png",
    });
    await expect(approveDraft(second, repo, APPROVAL)).rejects.toThrow(/duplicate content is not allowed/);
    expect(await repo.getStatus(second)).toBe("draft");
  });
});
