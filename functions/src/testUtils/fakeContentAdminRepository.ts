import { ContentStatus } from "../types";
import { ContentAdminRepositoryPort, NewDraftContent, StoredContent } from "../contentAdminRepositoryPort";
import type { PublicationRequest, PublicationResult, VersionedStoredContent } from "../contentAdminRepositoryPort";
import { practicalContentHash } from "../practicalLesson";

export class FakeContentAdminRepository implements ContentAdminRepositoryPort {
  docs = new Map<string, StoredContent>();
  private nextId = 1;
  private versions = new Map<string, number>();

  async createDraft(draft: NewDraftContent): Promise<string> {
    const id = `content-${this.nextId++}`;
    this.docs.set(id, { ...draft, status: "draft", createdAt: new Date(), updatedAt: new Date() });
    this.versions.set(id, 1);
    return id;
  }

  async getStatus(contentId: string): Promise<ContentStatus | null> {
    return this.docs.get(contentId)?.status ?? null;
  }

  async getContent(contentId: string): Promise<StoredContent | null> {
    return this.docs.get(contentId) ?? null;
  }

  async getVersionedContent(contentId: string): Promise<VersionedStoredContent | null> {
    const content = this.docs.get(contentId);
    const version = this.versions.get(contentId);
    return content && version ? { content, updateTime: `${version}:0` } : null;
  }

  async publishBatch(requests: PublicationRequest[]): Promise<PublicationResult[]> {
    return requests.map((request) => {
      const doc = this.docs.get(request.contentId);
      const version = this.versions.get(request.contentId);
      if (!doc || !version) throw new Error(`No fake doc '${request.contentId}'.`);
      if (doc.status === "published") return { contentId: request.contentId, outcome: "already-published" };
      if (`${version}:0` !== request.expectedUpdateTime || practicalContentHash(doc) !== request.expectedContentHash) {
        throw new Error(`Fake doc '${request.contentId}' changed after approval.`);
      }
      doc.status = "published";
      (doc as StoredContent & { contentStatus?: ContentStatus }).contentStatus = "published";
      this.versions.set(request.contentId, version + 1);
      return { contentId: request.contentId, outcome: "published" };
    });
  }

  async titleExists(title: string): Promise<boolean> {
    const normalized = title.trim().toLowerCase();
    return [...this.docs.values()].some((doc) => doc.title.trim().toLowerCase() === normalized);
  }
}
