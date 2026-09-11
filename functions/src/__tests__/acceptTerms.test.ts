import { acceptTerms, StalePolicyVersionError } from "../acceptTerms";
import { CURRENT_TERMS_VERSION, CURRENT_PRIVACY_VERSION } from "../policyVersions";
import { FakeUserProfileRepository } from "../testUtils/fakeUserProfileRepository";

const UID = "user-123";

describe("acceptTerms", () => {
  it("records acceptance of the current policy versions", async () => {
    const repo = new FakeUserProfileRepository();

    await acceptTerms(UID, CURRENT_TERMS_VERSION, CURRENT_PRIVACY_VERSION, repo);

    expect(repo.termsAcceptances).toEqual([
      { uid: UID, termsVersion: CURRENT_TERMS_VERSION, privacyVersion: CURRENT_PRIVACY_VERSION },
    ]);
  });

  it("rejects a stale terms version instead of recording it", async () => {
    const repo = new FakeUserProfileRepository();

    await expect(acceptTerms(UID, CURRENT_TERMS_VERSION - 1, CURRENT_PRIVACY_VERSION, repo)).rejects.toThrow(
      StalePolicyVersionError,
    );
    expect(repo.termsAcceptances).toHaveLength(0);
  });

  it("rejects a stale privacy version instead of recording it", async () => {
    const repo = new FakeUserProfileRepository();

    await expect(acceptTerms(UID, CURRENT_TERMS_VERSION, CURRENT_PRIVACY_VERSION - 1, repo)).rejects.toThrow(
      StalePolicyVersionError,
    );
    expect(repo.termsAcceptances).toHaveLength(0);
  });
});
