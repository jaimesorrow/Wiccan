import { UserProfileRepositoryPort } from "./userProfileRepositoryPort";
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
