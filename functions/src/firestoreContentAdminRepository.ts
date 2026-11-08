import { FieldValue, Firestore } from "firebase-admin/firestore";
import { ContentStatus } from "./types";
import { ContentAdminRepositoryPort, NewDraftContent, StoredContent } from "./contentAdminRepositoryPort";
import type { PublicationRequest, PublicationResult, VersionedStoredContent } from "./contentAdminRepositoryPort";
import { practicalContentHash } from "./practicalLesson";

const CONTENT_COLLECTION = "content";
const ORIGINALITY_RESERVATIONS_COLLECTION = "contentOriginalityReservations";
const PUBLICATION_AUDIT_COLLECTION = "contentPublicationAudit";
export const CONTENT_GENERATOR_ID = "ancient-grimoire-content-agent";
export const CONTENT_GENERATION_SOURCE_KIND = "historical-grimoire";

export class FirestoreContentAdminRepository implements ContentAdminRepositoryPort {
  constructor(private readonly db: Firestore) {}

  async createDraft(draft: NewDraftContent): Promise<string> {
    const doc = await this.db.collection(CONTENT_COLLECTION).add({
      ...draft,
      rightsReviewRequired: true,
      status: "draft" satisfies ContentStatus,
      contentStatus: "draft" satisfies ContentStatus,
      generatedBy: CONTENT_GENERATOR_ID,
      generationSourceKind: CONTENT_GENERATION_SOURCE_KIND,
      generatedAt: FieldValue.serverTimestamp(),
      createdAt: FieldValue.serverTimestamp(),
      updatedAt: FieldValue.serverTimestamp(),
    });
    return doc.id;
  }

  async createDraftWithOriginalityReservations(draft: NewDraftContent, reservationKeys: string[]): Promise<string> {
    if (reservationKeys.length === 0 || new Set(reservationKeys).size !== reservationKeys.length) {
      throw new Error("Originality reservations must be a non-empty unique list.");
    }
    const contentRef = this.db.collection(CONTENT_COLLECTION).doc();
    const reservationRefs = reservationKeys.map((key) =>
      this.db.collection(ORIGINALITY_RESERVATIONS_COLLECTION).doc(key));
    await this.db.runTransaction(async (transaction) => {
      const existing = await transaction.getAll(...reservationRefs);
      const collision = existing.find((snapshot) => snapshot.exists);
      if (collision) throw new Error(`Originality reservation collision: ${collision.id}`);
      transaction.create(contentRef, {
        ...draft,
        rightsReviewRequired: true,
        status: "draft" satisfies ContentStatus,
        contentStatus: "draft" satisfies ContentStatus,
        generatedBy: CONTENT_GENERATOR_ID,
        generationSourceKind: CONTENT_GENERATION_SOURCE_KIND,
        generatedAt: FieldValue.serverTimestamp(),
        createdAt: FieldValue.serverTimestamp(),
        updatedAt: FieldValue.serverTimestamp(),
      });
      reservationRefs.forEach((ref, index) => transaction.create(ref, {
        contentId: contentRef.id,
        reservationKey: reservationKeys[index],
        createdAt: FieldValue.serverTimestamp(),
      }));
    });
    return contentRef.id;
  }

  async listOriginalityInventory() {
    const snapshot = await this.db.collection(CONTENT_COLLECTION).get();
    return snapshot.docs.map((document) => ({ id: document.id, fields: document.data() }));
  }

  async getStatus(contentId: string): Promise<ContentStatus | null> {
    const doc = await this.db.collection(CONTENT_COLLECTION).doc(contentId).get();
    if (!doc.exists) return null;
    return (doc.get("status") as ContentStatus | undefined) ?? null;
  }

  async getContent(contentId: string): Promise<StoredContent | null> {
    const doc = await this.db.collection(CONTENT_COLLECTION).doc(contentId).get();
    if (!doc.exists) return null;
    return doc.data() as StoredContent;
  }

  async getVersionedContent(contentId: string): Promise<VersionedStoredContent | null> {
    const doc = await this.db.collection(CONTENT_COLLECTION).doc(contentId).get();
    if (!doc.exists || !doc.updateTime) return null;
    return {
      content: doc.data() as StoredContent,
      updateTime: `${doc.updateTime.seconds}:${doc.updateTime.nanoseconds}`,
    };
  }

  async publishBatch(requests: PublicationRequest[]): Promise<PublicationResult[]> {
    if (requests.length === 0 || requests.length > 25) throw new Error("Publication batches must contain 1 to 25 documents.");
    if (new Set(requests.map((request) => request.contentId)).size !== requests.length) throw new Error("Publication batch contains duplicate IDs.");
    return this.db.runTransaction(async (transaction) => {
      const refs = requests.map((request) => this.db.collection(CONTENT_COLLECTION).doc(request.contentId));
      const snapshots = await transaction.getAll(...refs);
      const results: PublicationResult[] = [];
      requests.forEach((request, index) => {
        const snapshot = snapshots[index];
        if (!snapshot.exists || !snapshot.updateTime) throw new Error(`Content '${request.contentId}' no longer exists.`);
        const content = snapshot.data() as StoredContent;
        const updateTime = `${snapshot.updateTime.seconds}:${snapshot.updateTime.nanoseconds}`;
        if (content.status === "published" && content.contentStatus === "published") {
          results.push({ contentId: request.contentId, outcome: "already-published" });
          return;
        }
        if (content.status !== "draft" || (content.contentStatus !== undefined && content.contentStatus !== "draft")) {
          throw new Error(`Content '${request.contentId}' is not an internally consistent draft.`);
        }
        if (updateTime !== request.expectedUpdateTime) throw new Error(`Content '${request.contentId}' changed after approval.`);
        if (practicalContentHash(content) !== request.expectedContentHash) throw new Error(`Content '${request.contentId}' hash changed after approval.`);
        if (!request.operator.trim() || !/^[a-f0-9]{64}$/.test(request.approvalDigest)) throw new Error("Publication operator and approval digest are required.");
        transaction.update(refs[index], {
          status: "published",
          contentStatus: "published",
          publishedAt: FieldValue.serverTimestamp(),
          updatedAt: FieldValue.serverTimestamp(),
        });
        const auditId = `${request.contentId}_${request.expectedContentHash}`;
        transaction.create(this.db.collection(PUBLICATION_AUDIT_COLLECTION).doc(auditId), {
          contentId: request.contentId,
          previousStatus: "draft",
          publishedStatus: "published",
          expectedUpdateTime: request.expectedUpdateTime,
          contentHash: request.expectedContentHash,
          operator: request.operator,
          approvalDigest: request.approvalDigest,
          publishedAt: FieldValue.serverTimestamp(),
        });
        results.push({ contentId: request.contentId, outcome: "published" });
      });
      return results;
    });
  }

  async titleExists(title: string): Promise<boolean> {
    const normalized = title.trim().toLowerCase();
    // A projection over every doc's title, compared in-memory rather than
    // via a Firestore query: Firestore has no case-insensitive operator,
    // and the curated pool (hundreds of entries, not millions) makes a
    // full scan of just this one field cheap and index-free.
    const snap = await this.db.collection(CONTENT_COLLECTION).select("title").get();
    return snap.docs.some((doc) => {
      const existing = doc.get("title");
      return typeof existing === "string" && existing.trim().toLowerCase() === normalized;
    });
  }
}
