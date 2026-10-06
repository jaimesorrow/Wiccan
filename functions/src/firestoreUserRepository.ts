import { Firestore, FieldValue } from "firebase-admin/firestore";
import { ContentTier } from "./types";
import {
  PremiumUserRecord,
  PurchaseTokenOwnershipError,
  TierSyncSource,
  UserRepositoryPort,
} from "./userRepositoryPort";
import { obfuscatedAccountId, purchaseTokenDocumentId } from "./billingIdentity";

const USERS_COLLECTION = "users";
const PURCHASE_TOKENS_COLLECTION = "purchaseTokens";
const USER_PURCHASE_TOKENS_COLLECTION = "userPurchaseTokens";

export class FirestoreUserRepository implements UserRepositoryPort {
  constructor(private readonly db: Firestore) {}

  async setTier(
    uid: string,
    tier: ContentTier,
    purchaseToken: string,
    source: TierSyncSource = "purchase",
    previousPurchaseToken?: string,
  ): Promise<ContentTier | null> {
    const userRef = this.db.collection(USERS_COLLECTION).doc(uid);
    const tokenRef = this.db.collection(PURCHASE_TOKENS_COLLECTION).doc(purchaseTokenDocumentId(purchaseToken));
    return this.db.runTransaction(async (transaction) => {
      const [user, token] = await Promise.all([transaction.get(userRef), transaction.get(tokenRef)]);
      if (!user.exists || user.get("deletionInProgress") === true) {
        if (source !== "purchase") return null;
        throw new PurchaseTokenOwnershipError("This account is unavailable or being deleted. Sign in again before purchasing.");
      }
      // Never let a delayed notification/recheck recreate a deleted account
      // or overwrite an upgrade that happened while the Play API request ran.
      const expectedToken = source === "replacement" ? previousPurchaseToken : purchaseToken;
      if (source !== "purchase" && (!expectedToken || user.get("purchaseToken") !== expectedToken)) {
        return null;
      }
      const owner = token.get("accountId") as string | undefined;
      if (owner && owner !== obfuscatedAccountId(uid)) {
        throw new PurchaseTokenOwnershipError("This purchase is already linked to another account.");
      }
      if (token.get("superseded") === true) {
        if (source !== "purchase") return null;
        throw new PurchaseTokenOwnershipError("This purchase has been replaced by a newer subscription.");
      }
      if (source !== "refresh" && previousPurchaseToken) {
        const linkedToken = await transaction.get(
          this.db.collection(PURCHASE_TOKENS_COLLECTION).doc(purchaseTokenDocumentId(previousPurchaseToken)),
        );
        const linkedOwner = linkedToken.get("accountId");
        if (linkedOwner && linkedOwner !== obfuscatedAccountId(uid)) {
          throw new PurchaseTokenOwnershipError("The previous subscription belongs to another account.");
        }
      }
      const currentPurchaseToken = user.get("purchaseToken");
      const outgoingToken = source !== "refresh" && typeof currentPurchaseToken === "string" &&
        currentPurchaseToken !== purchaseToken ? currentPurchaseToken : undefined;
      const outgoingRef = outgoingToken
        ? this.db.collection(PURCHASE_TOKENS_COLLECTION).doc(purchaseTokenDocumentId(outgoingToken)) : null;
      const outgoingOwnership = outgoingRef ? await transaction.get(outgoingRef) : null;
      const outgoingOwner = outgoingOwnership?.get("accountId");
      if (outgoingOwner && outgoingOwner !== obfuscatedAccountId(uid)) {
        throw new PurchaseTokenOwnershipError("The current subscription belongs to another account.");
      }
      if (!owner) {
        // Protect purchases recorded before the ownership registry existed.
        const legacyOwners = await transaction.get(
          this.db.collection(USERS_COLLECTION).where("purchaseToken", "==", purchaseToken).limit(2),
        );
        if (legacyOwners.docs.some((doc) => doc.id !== uid)) {
          throw new PurchaseTokenOwnershipError("This purchase is already linked to another account.");
        }
      }
      if (source !== "replacement" && source !== "refresh" && tier === "free" && user.get("tier") === "premium" &&
        user.get("purchaseToken") && user.get("purchaseToken") !== purchaseToken) {
        transaction.set(tokenRef, { accountId: obfuscatedAccountId(uid), updatedAt: FieldValue.serverTimestamp() }, { merge: true });
        transaction.set(this.userPurchaseTokenRef(uid, purchaseToken), {
          purchaseToken,
          updatedAt: FieldValue.serverTimestamp(),
        }, { merge: true });
        return "premium";
      }
      const previousRef = source !== "refresh" && previousPurchaseToken && previousPurchaseToken !== purchaseToken
        ? this.db.collection(PURCHASE_TOKENS_COLLECTION).doc(purchaseTokenDocumentId(previousPurchaseToken)) : null;
      const previousOwner = previousRef ? (await transaction.get(previousRef)).get("accountId") : undefined;
      // Only Play's linked predecessor is superseded. An independently
      // purchased subscription can still be restored on this same account.
      transaction.set(tokenRef, { accountId: obfuscatedAccountId(uid), updatedAt: FieldValue.serverTimestamp() }, { merge: true });
      if (outgoingRef) {
        transaction.set(outgoingRef, {
          accountId: obfuscatedAccountId(uid), updatedAt: FieldValue.serverTimestamp(),
        }, { merge: true });
        transaction.set(this.userPurchaseTokenRef(uid, outgoingToken!), {
          purchaseToken: outgoingToken,
          updatedAt: FieldValue.serverTimestamp(),
        }, { merge: true });
      }
      if (previousRef && (!previousOwner || previousOwner === obfuscatedAccountId(uid))) {
        transaction.set(previousRef, {
          accountId: obfuscatedAccountId(uid), superseded: true, updatedAt: FieldValue.serverTimestamp(),
        }, { merge: true });
      }
      transaction.set(this.userPurchaseTokenRef(uid, purchaseToken), {
        purchaseToken,
        updatedAt: FieldValue.serverTimestamp(),
      }, { merge: true });
      transaction.set(userRef, { tier, purchaseToken, updatedAt: FieldValue.serverTimestamp() }, { merge: true });
      return tier;
    });
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

  private userPurchaseTokenRef(uid: string, purchaseToken: string) {
    return this.db.collection(USER_PURCHASE_TOKENS_COLLECTION).doc(uid)
      .collection("tokens").doc(purchaseTokenDocumentId(purchaseToken));
  }
}
