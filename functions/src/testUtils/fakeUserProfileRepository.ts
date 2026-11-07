import { AcceptedPolicyVersions, UserProfileRepositoryPort } from "../userProfileRepositoryPort";

export class FakeUserProfileRepository implements UserProfileRepositoryPort {
  createdProfiles: string[] = [];
  acceptedTermsAt = new Map<string, boolean>();
  termsAcceptances: { uid: string; termsVersion: number; privacyVersion: number }[] = [];
  acceptedVersions = new Map<string, AcceptedPolicyVersions>();

  async createProfile(uid: string): Promise<void> {
    this.createdProfiles.push(uid);
  }

  async acceptTerms(uid: string, termsVersion: number, privacyVersion: number): Promise<void> {
    this.acceptedTermsAt.set(uid, true);
    this.termsAcceptances.push({ uid, termsVersion, privacyVersion });
    this.acceptedVersions.set(uid, { termsVersion, privacyVersion });
  }

  async getAcceptedPolicyVersions(uid: string): Promise<AcceptedPolicyVersions> {
    return this.acceptedVersions.get(uid) ?? { termsVersion: null, privacyVersion: null };
  }
}
