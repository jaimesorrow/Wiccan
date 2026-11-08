import { UserProfileRepositoryPort } from "./userProfileRepositoryPort";

export type PolicyAcceptanceReader = Pick<UserProfileRepositoryPort, "getAcceptedPolicyVersions">;
import { CURRENT_TERMS_VERSION, CURRENT_PRIVACY_VERSION } from "./policyVersions";

export class StalePolicyVersionError extends Error {
  constructor() {
    super(
      `Client is quoting an outdated policy version. Current versions are ` +
        `terms=${CURRENT_TERMS_VERSION}, privacy=${CURRENT_PRIVACY_VERSION}.`,
    );
    this.name = "StalePolicyVersionError";
  }
}

/**
 * Records the caller's Terms/Privacy acceptance. Only ever accepts the
 * current policy versions - a client quoting anything else (an
 * un-updated app build, or a tampered request) is rejected rather than
 * silently recorded, since a stale acceptance would be meaningless for
 * compliance purposes.
 */
export async function acceptTerms(
  uid: string,
  termsVersion: number,
  privacyVersion: number,
  repo: UserProfileRepositoryPort,
): Promise<void> {
  if (termsVersion !== CURRENT_TERMS_VERSION || privacyVersion !== CURRENT_PRIVACY_VERSION) {
    throw new StalePolicyVersionError();
  }
  await repo.acceptTerms(uid, termsVersion, privacyVersion);
}

export class PolicyAcceptanceRequiredError extends Error {
  constructor() {
    super("Review and accept the current Terms of Service and Privacy Policy in the app to continue.");
    this.name = "PolicyAcceptanceRequiredError";
  }
}

/**
 * True only when the user's recorded acceptance matches the server's
 * current Terms and Privacy versions. Read from Firestore, never from the
 * client, so a modified app cannot skip the onboarding agreement gate.
 */
export async function hasAcceptedCurrentPolicies(uid: string, repo: PolicyAcceptanceReader): Promise<boolean> {
  const accepted = await repo.getAcceptedPolicyVersions(uid);
  return accepted.termsVersion === CURRENT_TERMS_VERSION && accepted.privacyVersion === CURRENT_PRIVACY_VERSION;
}

export async function assertCurrentPoliciesAccepted(uid: string, repo: PolicyAcceptanceReader): Promise<void> {
  if (!(await hasAcceptedCurrentPolicies(uid, repo))) {
    throw new PolicyAcceptanceRequiredError();
  }
}
