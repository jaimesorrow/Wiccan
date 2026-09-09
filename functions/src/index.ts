import { initializeApp } from "firebase-admin/app";
import { getFirestore } from "firebase-admin/firestore";
import { onCall, HttpsError } from "firebase-functions/v2/https";
import { logger } from "firebase-functions";

initializeApp();

export type ContentType = "spell" | "potion" | "lesson";
export type ContentTier = "free" | "premium";
export type ContentStatus = "draft" | "published";

export interface Content {
  id: string;
  type: ContentType;
  tier: ContentTier;
  tradition: string;
  title: string;
  intro: string;
  body: string;
  materials?: string[];
  safetyNote?: string | null;
  tags: string[];
  status: ContentStatus;
}

export interface UserContentHistory {
  userId: string;
  contentId: string;
  deliveredDate: string;
}

export interface ContentStore {
  getTodayContent(userId: string, deliveredDate: string): Promise<Content | null>;
  getUserTier(userId: string): Promise<ContentTier>;
  listPublishedContent(): Promise<Content[]>;
  listDeliveredContentIds(userId: string): Promise<string[]>;
  writeHistory(entry: UserContentHistory): Promise<void>;
}

export interface LoggerLike {
  warn(message: string, metadata?: Record<string, unknown>): void;
}

const firestoreStore: ContentStore = {
  async getTodayContent(userId, deliveredDate) {
    const db = getFirestore();
    const historySnapshot = await db
      .collection("userContentHistory")
      .where("userId", "==", userId)
      .where("deliveredDate", "==", deliveredDate)
      .limit(1)
      .get();

    const history = historySnapshot.docs[0]?.data() as UserContentHistory | undefined;
    if (!history) {
      return null;
    }

    const contentSnapshot = await db.collection("content").doc(history.contentId).get();
    if (!contentSnapshot.exists) {
      return null;
    }

    return mapContent(contentSnapshot.id, contentSnapshot.data());
  },
  async getUserTier(userId) {
    const userSnapshot = await getFirestore().collection("users").doc(userId).get();
    const tier = userSnapshot.get("subscriptionTier") ?? userSnapshot.get("tier");
    return tier === "premium" ? "premium" : "free";
  },
  async listPublishedContent() {
    const snapshot = await getFirestore()
      .collection("content")
      .where("status", "==", "published")
      .get();

    return snapshot.docs.map((doc) => mapContent(doc.id, doc.data()));
  },
  async listDeliveredContentIds(userId) {
    const snapshot = await getFirestore()
      .collection("userContentHistory")
      .where("userId", "==", userId)
      .get();

    return snapshot.docs
      .map((doc) => doc.get("contentId"))
      .filter((contentId): contentId is string => typeof contentId === "string");
  },
  async writeHistory(entry) {
    await getFirestore().collection("userContentHistory").add(entry);
  },
};

export async function getTodayContentForUser({
  uid,
  store,
  loggerInstance = logger,
  today = currentIsoDate(),
  random = Math.random,
}: {
  uid: string;
  store: ContentStore;
  loggerInstance?: LoggerLike;
  today?: string;
  random?: () => number;
}): Promise<Content> {
  const existing = await store.getTodayContent(uid, today);
  if (existing) {
    return existing;
  }

  const [tier, publishedContent, deliveredIds] = await Promise.all([
    store.getUserTier(uid),
    store.listPublishedContent(),
    store.listDeliveredContentIds(uid),
  ]);

  const deliveredSet = new Set(deliveredIds);
  const eligiblePool = publishedContent.filter((content) => isEligibleForTier(content, tier));
  const undistributedPool = eligiblePool.filter((content) => !deliveredSet.has(content.id));
  const candidatePool = undistributedPool.length > 0 ? undistributedPool : eligiblePool;

  if (candidatePool.length === 0) {
    throw new HttpsError("failed-precondition", "No published content is available.");
  }

  if (undistributedPool.length === 0) {
    loggerInstance.warn("Published content pool exhausted for user", { uid, tier, today });
  }

  const selectedContent = candidatePool[Math.floor(random() * candidatePool.length)];

  await store.writeHistory({
    userId: uid,
    contentId: selectedContent.id,
    deliveredDate: today,
  });

  return selectedContent;
}

export const getTodayContent = onCall(async (request) => {
  if (!request.auth?.uid) {
    throw new HttpsError("unauthenticated", "Authentication is required.");
  }

  const requestedUid = typeof request.data?.uid === "string" ? request.data.uid : request.auth.uid;
  if (requestedUid !== request.auth.uid) {
    throw new HttpsError("permission-denied", "uid must match the authenticated user.");
  }

  return getTodayContentForUser({
    uid: request.auth.uid,
    store: firestoreStore,
  });
});

function isEligibleForTier(content: Content, tier: ContentTier): boolean {
  return tier === "premium" || content.tier === "free";
}

function mapContent(id: string, data: unknown): Content {
  const source = data as Record<string, unknown> | undefined;
  if (!source) {
    throw new HttpsError("data-loss", `Content document ${id} is missing data.`);
  }

  return {
    id,
    type: asContentType(source.type),
    tier: asContentTier(source.tier),
    tradition: asString(source.tradition, "tradition"),
    title: asString(source.title, "title"),
    intro: asString(source.intro, "intro"),
    body: asString(source.body, "body"),
    materials: asOptionalStringArray(source.materials),
    safetyNote: typeof source.safetyNote === "string" ? source.safetyNote : null,
    tags: asStringArray(source.tags, "tags"),
    status: asContentStatus(source.status),
  };
}

function asString(value: unknown, field: string): string {
  if (typeof value !== "string") {
    throw new HttpsError("data-loss", `Expected ${field} to be a string.`);
  }
  return value;
}

function asStringArray(value: unknown, field: string): string[] {
  if (!Array.isArray(value) || value.some((item) => typeof item !== "string")) {
    throw new HttpsError("data-loss", `Expected ${field} to be a string array.`);
  }
  return value;
}

function asOptionalStringArray(value: unknown): string[] | undefined {
  if (value == null) {
    return undefined;
  }
  return asStringArray(value, "materials");
}

function asContentType(value: unknown): ContentType {
  if (value === "spell" || value === "potion" || value === "lesson") {
    return value;
  }
  throw new HttpsError("data-loss", "Unexpected content type.");
}

function asContentTier(value: unknown): ContentTier {
  if (value === "free" || value === "premium") {
    return value;
  }
  throw new HttpsError("data-loss", "Unexpected content tier.");
}

function asContentStatus(value: unknown): ContentStatus {
  if (value === "draft" || value === "published") {
    return value;
  }
  throw new HttpsError("data-loss", "Unexpected content status.");
}

function currentIsoDate(date = new Date()): string {
  return date.toISOString().slice(0, 10);
}
