import { DocumentSnapshot } from "firebase-admin/firestore";

export class AccountWriteUnavailableError extends Error {
  constructor() {
    super("This account is unavailable or being deleted. Sign in again or retry account deletion.");
    this.name = "AccountWriteUnavailableError";
  }
}

/** Check within the write's transaction so account deletion cannot race this write. */
export function assertAccountWriteAllowed(user: DocumentSnapshot): void {
  if (!user.exists || user.get("deletionInProgress") === true) throw new AccountWriteUnavailableError();
}
