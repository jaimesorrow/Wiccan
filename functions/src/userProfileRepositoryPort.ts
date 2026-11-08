/** The Terms/Privacy versions a user last accepted; null where never accepted. */
export interface AcceptedPolicyVersions {
  termsVersion: number | null;
  privacyVersion: number | null;
}

export interface UserProfileRepositoryPort {
  createProfile(uid: string): Promise<void>;

  /** Reads the recorded acceptance; both null for a missing profile or one that never accepted. */
  getAcceptedPolicyVersions(uid: string): Promise<AcceptedPolicyVersions>;

  /**
   * Records the caller's Terms/Privacy acceptance. `acceptedTermsAt` is
   * set only the first time a user ever accepts (an audit record of
   * original acceptance); `termsVersion`/`privacyVersion` are recorded
   * on every call, including re-acceptance of an updated policy.
   */
  acceptTerms(uid: string, termsVersion: number, privacyVersion: number): Promise<void>;
}
