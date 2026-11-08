import {
  acceptTerms,
  assertCurrentPoliciesAccepted,
  hasAcceptedCurrentPolicies,
  PolicyAcceptanceRequiredError,
  StalePolicyVersionError,
} from "../acceptTerms";
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

describe("current policy acceptance gate", () => {
  it("passes only after the current versions are recorded", async () => {
    const repo = new FakeUserProfileRepository();
    await expect(assertCurrentPoliciesAccepted(UID, repo)).rejects.toThrow(PolicyAcceptanceRequiredError);

    await acceptTerms(UID, CURRENT_TERMS_VERSION, CURRENT_PRIVACY_VERSION, repo);

    await expect(assertCurrentPoliciesAccepted(UID, repo)).resolves.toBeUndefined();
  });

  it("treats an older accepted terms or privacy version as not accepted", async () => {
    const repo = new FakeUserProfileRepository();
    repo.acceptedVersions.set("old-terms", { termsVersion: CURRENT_TERMS_VERSION - 1, privacyVersion: CURRENT_PRIVACY_VERSION });
    repo.acceptedVersions.set("old-privacy", { termsVersion: CURRENT_TERMS_VERSION, privacyVersion: CURRENT_PRIVACY_VERSION - 1 });

    expect(await hasAcceptedCurrentPolicies("old-terms", repo)).toBe(false);
    expect(await hasAcceptedCurrentPolicies("old-privacy", repo)).toBe(false);
  });
});
