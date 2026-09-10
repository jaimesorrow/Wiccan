import { ContentStatus } from "../types";
import { ContentAdminRepositoryPort, NewDraftContent } from "../contentAdminRepositoryPort";

export class FakeContentAdminRepository implements ContentAdminRepositoryPort {
  docs = new Map<string, NewDraftContent & { status: ContentStatus }>();
  private nextId = 1;

  async createDraft(draft: NewDraftContent): Promise<string> {
    const id = `content-${this.nextId++}`;
    this.docs.set(id, { ...draft, status: "draft" });
    return id;
  }

  async getStatus(contentId: string): Promise<ContentStatus | null> {
    return this.docs.get(contentId)?.status ?? null;
  }

  async publish(contentId: string): Promise<void> {
    const doc = this.docs.get(contentId);
    if (!doc) throw new Error(`No fake doc '${contentId}'.`);
    doc.status = "published";
  }
}
