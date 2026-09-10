import { createUserProfileOnSignUp } from "../createUserProfile";
import { FakeUserProfileRepository } from "../testUtils/fakeUserProfileRepository";

describe("createUserProfileOnSignUp", () => {
  it("creates a profile for the given uid", async () => {
    const repo = new FakeUserProfileRepository();

    await createUserProfileOnSignUp("user-123", repo);

    expect(repo.createdProfiles).toEqual(["user-123"]);
  });
});
