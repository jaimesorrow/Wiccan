import { Firestore, Timestamp } from "firebase-admin/firestore";
import { Content, ContentCategory, ContentTier, isContentCategory } from "./types";
import { ContentAlreadyDeliveredError, ContentRepositoryPort } from "./contentRepositoryPort";

/** Defensive fallback for any legacy content document written before category existed. */
const DEFAULT_CATEGORY: ContentCategory = "MoonAndSeasonal";

const CONTENT_COLLECTION = "content";
const HISTORY_COLLECTION = "userContentHistory";
const CONTENT_USAGE_COLLECTION = "userContentUsage";
const USERS_COLLECTION = "users";
const POOL_EXHAUSTION_COLLECTION = "contentPoolExhaustionEvents";

/** Tiers a user on `tier` is entitled to see (premium also gets free). */
function eligibleTiers(tier: ContentTier): ContentTier[] {
  return tier === "premium" ? ["free", "premium"] : ["free"];
}

/**
 * Deterministic per-user-per-day history document ID. Giving every
 * caller racing for the same (uid, today) the same document identity is
 * what lets `assignDailyContent`'s transaction guarantee a single winner:
 * two concurrent transactions targeting the same doc always serialize.
 */
function historyDocId(uid: string, today: string): string {
  return `${uid}_${today}`;
}

function toContent(id: string, data: FirebaseFirestore.DocumentData): Content {
  return {
    id,
    type: data.type,
    tier: data.tier,
    tradition: data.tradition,
    category: isContentCategory(data.category) ? data.category : DEFAULT_CATEGORY,
    title: data.title,
    intro: data.intro,
    body: data.body,
    reflection: data.reflection ?? null,
    practice: data.practice ?? null,
    materials: data.materials ?? null,
    safetyNote: data.safetyNote ?? null,
    tags: data.tags ?? [],
    difficulty: data.difficulty,
    status: data.status,
    sourceId: typeof data.sourceId === "string" ? data.sourceId : undefined,
  };
}

export class FirestoreContentRepository implements ContentRepositoryPort {
  constructor(private readonly db: Firestore) {}

  async getUserTier(uid: string): Promise<ContentTier> {
    const snap = await this.db.collection(USERS_COLLECTION).doc(uid).get();
    return snap.get("tier") === "premium" ? "premium" : "free";
  }

  async getUserTimeZone(uid: string): Promise<string | null> {
    const snap = await this.db.collection(USERS_COLLECTION).doc(uid).get();
    const timeZone = snap.get("timeZone");
    return typeof timeZone === "string" ? timeZone : null;
  }

  async findTodayDeliveredContentId(uid: string, today: string): Promise<string | null> {
    const doc = await this.db.collection(HISTORY_COLLECTION).doc(historyDocId(uid, today)).get();
    return doc.exists ? (doc.get("contentId") as string) : null;
  }

  async getDeliveredContentIds(uid: string): Promise<string[]> {
    const snap = await this.db.collection(HISTORY_COLLECTION).where("userId", "==", uid).get();
    return snap.docs.map((doc) => doc.get("contentId") as string);
  }

  async getContentById(id: string): Promise<Content | null> {
    const doc = await this.db.collection(CONTENT_COLLECTION).doc(id).get();
    if (!doc.exists) return null;
    return toContent(doc.id, doc.data()!);
  }

  async getPublishedContentForTier(tier: ContentTier): Promise<Content[]> {
    const snap = await this.db
      .collection(CONTENT_COLLECTION)
      .where("status", "==", "published")
      .where("tier", "in", eligibleTiers(tier))
      .get();
    return snap.docs.map((doc) => toContent(doc.id, doc.data()));
  }

  async assignDailyContent(uid: string, today: string, candidate: Content): Promise<string> {
    const docRef = this.db.collection(HISTORY_COLLECTION).doc(historyDocId(uid, today));
    const usedRef = this.db.collection(CONTENT_USAGE_COLLECTION).doc(uid).collection("content").doc(candidate.id);
    return this.db.runTransaction(async (txn) => {
      const snap = await txn.get(docRef);
      if (snap.exists) {
        return snap.get("contentId") as string;
      }
      const used = await txn.get(usedRef);
      if (used.exists) {
        throw new ContentAlreadyDeliveredError(candidate.id);
      }
      txn.set(docRef, {
        userId: uid,
        contentId: candidate.id,
        deliveredDate: today,
        title: candidate.title,
        type: candidate.type,
        tradition: candidate.tradition,
        category: candidate.category,
      });
      txn.set(usedRef, { deliveredDate: today });
      return candidate.id;
    });
  }

  async logPoolExhaustion(uid: string, tier: ContentTier, today: string): Promise<void> {
    await this.db.collection(POOL_EXHAUSTION_COLLECTION).add({
      userId: uid,
      tier,
      date: today,
      loggedAt: Timestamp.now(),
    });
  }
}
