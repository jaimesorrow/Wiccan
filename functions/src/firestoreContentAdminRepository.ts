import { Firestore } from "firebase-admin/firestore";
import { ContentStatus } from "./types";
import { ContentAdminRepositoryPort, NewDraftContent, StoredContent } from "./contentAdminRepositoryPort";

const CONTENT_COLLECTION = "content";

export class FirestoreContentAdminRepository implements ContentAdminRepositoryPort {
  constructor(private readonly db: Firestore) {}

  async createDraft(draft: NewDraftContent): Promise<string> {
    const doc = await this.db.collection(CONTENT_COLLECTION).add({
      ...draft,
      status: "draft" satisfies ContentStatus,
    });
    return doc.id;
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

  async publish(contentId: string): Promise<void> {
    await this.db.collection(CONTENT_COLLECTION).doc(contentId).update({ status: "published" });
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
