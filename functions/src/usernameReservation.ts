import { parseUsername } from "./callableInput";

export const USERNAMES_COLLECTION = "usernames";

export class UsernameTakenError extends Error {
  constructor() {
    super("That username is already reserved.");
    this.name = "UsernameTakenError";
  }
}

export class UsernameRequiredError extends Error {
  constructor() {
    super("A valid username is required before a profile can be completed.");
    this.name = "UsernameRequiredError";
  }
}

export interface UsernameReservationDecision {
  displayUsername: string;
  key: string;
  /** Previous reservation to release when the caller changes their own handle. */
  previousKey: string | null;
}

/**
 * Decides the reservation write for one account. A profile must not be marked
 * complete unless this returns a handle. An existing reservation owned by
 * someone else rejects the write; the caller's other profile fields stay put.
 */
export function decideUsernameReservation(input: {
  uid: string;
  requested: string | undefined;
  currentUsername: unknown;
  reservedByUid: string | null;
}): UsernameReservationDecision {
  const current = typeof input.currentUsername === "string" ? parseUsername(input.currentUsername) : null;
  const requested = input.requested === undefined ? current : parseUsername(input.requested);
  if (!requested) throw new UsernameRequiredError();
  if (input.reservedByUid && input.reservedByUid !== input.uid) throw new UsernameTakenError();
  const key = requested.toLowerCase();
  const previousKey = current && current.toLowerCase() !== key ? current.toLowerCase() : null;
  return { displayUsername: requested, key, previousKey };
}
