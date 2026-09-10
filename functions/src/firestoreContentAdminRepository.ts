import { Firestore } from "firebase-admin/firestore";
import { ContentStatus } from "./types";
import { ContentAdminRepositoryPort, NewDraftContent } from "./contentAdminRepositoryPort";

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

  async publish(contentId: string): Promise<void> {
    await this.db.collection(CONTENT_COLLECTION).doc(contentId).update({ status: "published" });
  }
}
