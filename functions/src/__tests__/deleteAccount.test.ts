import { deleteAccount } from "../deleteAccount";
import { FakeAccountDeletionRepository } from "../testUtils/fakeAccountDeletionRepository";

describe("deleteAccount", () => {
  it("deletes the profile, then history, then the Auth account, in order", async () => {
    const repo = new FakeAccountDeletionRepository();

    await deleteAccount("user-123", repo);

    expect(repo.calls).toEqual(["profile:user-123", "history:user-123", "auth:user-123"]);
  });
});
