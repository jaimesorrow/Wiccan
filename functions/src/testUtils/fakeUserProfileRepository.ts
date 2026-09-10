import { UserProfileRepositoryPort } from "../userProfileRepositoryPort";

export class FakeUserProfileRepository implements UserProfileRepositoryPort {
  createdProfiles: string[] = [];

  async createProfile(uid: string): Promise<void> {
    this.createdProfiles.push(uid);
  }
}
