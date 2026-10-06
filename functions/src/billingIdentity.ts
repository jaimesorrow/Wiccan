import { createHash } from "node:crypto";

/** Matches Android's BillingFlowParams obfuscated account ID; never send the raw uid to Play. */
export function obfuscatedAccountId(uid: string): string {
  return createHash("sha256").update(uid, "utf8").digest("hex");
}

/** Tokens may contain characters forbidden in Firestore document IDs. */
export function purchaseTokenDocumentId(purchaseToken: string): string {
  return createHash("sha256").update(purchaseToken, "utf8").digest("hex");
}
