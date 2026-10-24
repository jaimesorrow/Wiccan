import { Firestore, Timestamp } from "firebase-admin/firestore";
import { Content, ContentCategory, ContentTier, isContentCategory } from "./types";
import { ContentAlreadyDeliveredError, ContentNoLongerEligibleError, ContentRepositoryPort, SubscriptionRequiredError } from "./contentRepositoryPort";
import { contentSimilarityKey, isEligibleForDailyDelivery, sharesContentSimilarity, similarityReservationId } from "./contentEligibility";
import { assertAccountWriteAllowed } from "./accountWriteGuard";
import { hasSubscriptionAccess } from "./subscriptionAccess";

/** Defensive fallback for any legacy content document written before category existed. */
const DEFAULT_CATEGORY: ContentCategory = "MoonAndSeasonal";

const CONTENT_COLLECTION = "content";
const HISTORY_COLLECTION = "userContentHistory";
const CONTENT_USAGE_COLLECTION = "userContentUsage";
const USERS_COLLECTION = "users";
const POOL_EXHAUSTION_COLLECTION = "contentPoolExhaustionEvents";

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
    sourceTradition: typeof data.sourceTradition === "string" ? data.sourceTradition : undefined,
    category: isContentCategory(data.primaryCategoryId) ? data.primaryCategoryId :
      isContentCategory(data.category) ? data.category : DEFAULT_CATEGORY,
    title: data.title,
    intro: data.intro,
    body: data.body,
    reflection: data.reflection ?? null,
    practice: data.practice ?? null,
    materials: data.materials ?? null,
    safetyNote: data.safetyNote ?? null,
    tags: data.tags ?? [],
    status: data.status,
    sourceId: typeof data.sourceId === "string" ? data.sourceId : undefined,
    sourceCitation: typeof data.sourceCitation === "string" ? data.sourceCitation : undefined,
    sourceUrl: typeof data.sourceUrl === "string" ? data.sourceUrl : undefined,
    rightsReviewRequired: typeof data.rightsReviewRequired === "boolean" ? data.rightsReviewRequired : undefined,
    rightsNotes: typeof data.rightsNotes === "string" ? data.rightsNotes : null,
    similarityGroupId: typeof data.similarityGroupId === "string" ? data.similarityGroupId : undefined,
    summary: typeof data.summary === "string" ? data.summary : undefined,
    primaryCategoryId: typeof data.primaryCategoryId === "string" ? data.primaryCategoryId : undefined,
    secondaryCategoryIds: Array.isArray(data.secondaryCategoryIds) ? data.secondaryCategoryIds : [],
    lessonType: typeof data.lessonType === "string" ? data.lessonType : undefined,
    requiresPotionDisclosure: data.requiresPotionDisclosure === true,
    durationMinutes: typeof data.durationMinutes === "number" ? data.durationMinutes : null,
    tools: Array.isArray(data.tools) ? data.tools : data.materials ?? [],
    incantation: typeof data.incantation === "string" ? data.incantation : null,
    incantationOmissionReason: typeof data.incantationOmissionReason === "string" ? data.incantationOmissionReason : null,
    timing: typeof data.timing === "string" ? data.timing : null,
    application: typeof data.application === "string" ? data.application : undefined,
    correspondences: Array.isArray(data.correspondences) ? data.correspondences : [],
    citations: Array.isArray(data.citations) ? data.citations : [],
    adaptationNotes: Array.isArray(data.adaptationNotes) ? data.adaptationNotes : [],
    sourcePassageId: typeof data.sourcePassageId === "string" ? data.sourcePassageId : undefined,
    practicalReview: data.practicalReview,
  };
}

export class FirestoreContentRepository implements ContentRepositoryPort {
  constructor(private readonly db: Firestore) {}

  async getUserTier(uid: string): Promise<ContentTier> {
    const snap = await this.db.collection(USERS_COLLECTION).doc(uid).get();
    return hasSubscriptionAccess(snap.data()) ? "premium" : "free";
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
    if (tier !== "premium") return [];
    const snap = await this.db
      .collection(CONTENT_COLLECTION)
      .where("status", "==", "published")
      .get();
    return snap.docs.map((doc) => toContent(doc.id, doc.data()));
  }

  async assignDailyContent(uid: string, today: string, candidate: Content, ancientLessonSourceIds: ReadonlySet<string>): Promise<string> {
    const docRef = this.db.collection(HISTORY_COLLECTION).doc(historyDocId(uid, today));
    const usageRef = this.db.collection(CONTENT_USAGE_COLLECTION).doc(uid);
    const usedRef = usageRef.collection("content").doc(candidate.id);
    return this.db.runTransaction(async (txn) => {
      const user = await txn.get(this.db.collection(USERS_COLLECTION).doc(uid));
      assertAccountWriteAllowed(user);
      if (!hasSubscriptionAccess(user.data())) throw new SubscriptionRequiredError();
      const snap = await txn.get(docRef);
      if (snap.exists) {
        return snap.get("contentId") as string;
      }
      const used = await txn.get(usedRef);
      if (used.exists) {
        throw new ContentAlreadyDeliveredError(candidate.id);
      }
      // Every new assignment also reads/writes one per-user document.
      // This serializes cross-date delivery even when old passage-based
      // metadata and newer explicit groups have different reservation keys,
      // without relying on a history query to detect phantom insertions.
      await txn.get(usageRef);
      // Revalidate authoritative entitlement and publication in the same
      // transaction as delivery, never trusting the earlier pool snapshot.
      const [currentDocument, history] = await Promise.all([
        txn.get(this.db.collection(CONTENT_COLLECTION).doc(candidate.id)),
        txn.get(this.db.collection(HISTORY_COLLECTION).where("userId", "==", uid)),
      ]);
      if (!currentDocument.exists) throw new ContentNoLongerEligibleError(candidate.id);
      const current = toContent(currentDocument.id, currentDocument.data()!);
      if (current.status !== "published" ||
          !isEligibleForDailyDelivery(current, ancientLessonSourceIds)) {
        throw new ContentNoLongerEligibleError(candidate.id);
      }
      if (history.docs.some((entry) => entry.get("contentId") === candidate.id)) {
        // Upgrades retain old history rows, including those written before
        // per-content reservations existed. They must still prevent repeats.
        throw new ContentAlreadyDeliveredError(candidate.id);
      }

      const similarityKey = contentSimilarityKey(current);
      const groupRef = similarityKey ? usageRef.collection("similarityGroups").doc(similarityReservationId(similarityKey)) : null;
      if (groupRef) {
        if ((await txn.get(groupRef)).exists) throw new ContentAlreadyDeliveredError(candidate.id);
        if (history.docs.some((entry) => entry.get("similarityGroupId") &&
            contentSimilarityKey({ similarityGroupId: entry.get("similarityGroupId") }) === similarityKey)) {
          throw new ContentAlreadyDeliveredError(candidate.id);
        }
        // Legacy history has no grouping fields. Resolve its existing
        // content documents so newly added editorial metadata also protects
        // users who received lessons before this change.
        const historicalIds = [...new Set(history.docs.map((entry) => entry.get("contentId")))]
          .filter((id): id is string => typeof id === "string" && id.length > 0);
        if (historicalIds.length > 0) {
          const historicalContent = await txn.getAll(...historicalIds.map((id) =>
            this.db.collection(CONTENT_COLLECTION).doc(id)));
          const historicalById = new Map(historicalContent.map((entry) => [entry.id, entry]));
          if (history.docs.some((entry) => {
            const content = historicalById.get(entry.get("contentId"));
            if (content?.exists) {
              return sharesContentSimilarity(toContent(content.id, content.data()!), current);
            }
            // A deleted historical document cannot resolve newer editorial
            // groups. Keep its recorded passage as a conservative fallback.
            return sharesContentSimilarity({
              similarityGroupId: entry.get("similarityGroupId"),
              sourceId: entry.get("sourceId"), sourceCitation: entry.get("sourceCitation"),
            }, current);
          })) {
            throw new ContentAlreadyDeliveredError(candidate.id);
          }
        }
      }
      if (!hasSubscriptionAccess(user.data())) throw new SubscriptionRequiredError();
      txn.set(docRef, {
        userId: uid,
        contentId: candidate.id,
        deliveredDate: today,
        title: current.title,
        type: current.type,
        tradition: current.tradition,
        category: current.category,
        ...(current.similarityGroupId ? { similarityGroupId: current.similarityGroupId } : {}),
        ...(current.sourceId ? { sourceId: current.sourceId } : {}),
        ...(current.sourceCitation ? { sourceCitation: current.sourceCitation } : {}),
      });
      txn.set(usedRef, { deliveredDate: today });
      if (groupRef) txn.set(groupRef, { deliveredDate: today, contentId: candidate.id });
      txn.set(usageRef, { lastDeliveredDate: today }, { merge: true });
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
