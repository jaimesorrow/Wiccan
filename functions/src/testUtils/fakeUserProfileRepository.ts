import { UserProfileRepositoryPort } from "../userProfileRepositoryPort";

export class FakeUserProfileRepository implements UserProfileRepositoryPort {
  createdProfiles: string[] = [];
  acceptedTermsAt = new Map<string, boolean>();
  termsAcceptances: { uid: string; termsVersion: number; privacyVersion: number }[] = [];

  async createProfile(uid: string): Promise<void> {
    this.createdProfiles.push(uid);
  }

  async acceptTerms(uid: string, termsVersion: number, privacyVersion: number): Promise<void> {
    this.acceptedTermsAt.set(uid, true);
    this.termsAcceptances.push({ uid, termsVersion, privacyVersion });
  }
}
