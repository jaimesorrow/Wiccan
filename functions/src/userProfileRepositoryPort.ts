export interface UserProfileRepositoryPort {
  createProfile(uid: string): Promise<void>;

  /**
   * Records the caller's Terms/Privacy acceptance. `acceptedTermsAt` is
   * set only the first time a user ever accepts (an audit record of
   * original acceptance); `termsVersion`/`privacyVersion` are recorded
   * on every call, including re-acceptance of an updated policy.
   */
  acceptTerms(uid: string, termsVersion: number, privacyVersion: number): Promise<void>;
}
