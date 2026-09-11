import { Firestore, FieldValue } from "firebase-admin/firestore";
import { ContentTier } from "./types";
import { PremiumUserRecord, UserRepositoryPort } from "./userRepositoryPort";

const USERS_COLLECTION = "users";

export class FirestoreUserRepository implements UserRepositoryPort {
  constructor(private readonly db: Firestore) {}

  async setTier(uid: string, tier: ContentTier, purchaseToken?: string): Promise<void> {
    const data: Record<string, unknown> = {
      tier,
      updatedAt: FieldValue.serverTimestamp(),
    };
    if (purchaseToken) {
      data.purchaseToken = purchaseToken;
    }
    await this.db.collection(USERS_COLLECTION).doc(uid).set(data, { merge: true });
  }

  async listPremiumUsers(): Promise<PremiumUserRecord[]> {
    const snap = await this.db.collection(USERS_COLLECTION).where("tier", "==", "premium").get();
    return snap.docs
      .filter((doc) => typeof doc.get("purchaseToken") === "string")
      .map((doc) => ({ uid: doc.id, purchaseToken: doc.get("purchaseToken") as string }));
  }

  async findUidByPurchaseToken(purchaseToken: string): Promise<string | null> {
    const snap = await this.db
      .collection(USERS_COLLECTION)
      .where("purchaseToken", "==", purchaseToken)
      .limit(1)
      .get();
    return snap.empty ? null : snap.docs[0].id;
  }
}
