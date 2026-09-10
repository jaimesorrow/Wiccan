export interface UserProfileRepositoryPort {
  createProfile(uid: string): Promise<void>;
}
