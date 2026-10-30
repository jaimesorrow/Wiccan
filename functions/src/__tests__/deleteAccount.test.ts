import { deleteAccount } from "../deleteAccount";
import { FakeAccountDeletionRepository } from "../testUtils/fakeAccountDeletionRepository";

describe("deleteAccount", () => {
  it("deletes history, devices, the subscription record, the profile, then the Auth account, in order", async () => {
    const repo = new FakeAccountDeletionRepository();

    await deleteAccount("user-123", repo);

    expect(repo.calls).toEqual([
      "cancel-subscription:user-123",
      "history:user-123",
      "usage:user-123",
      "devices:user-123",
      "subscription:user-123",
      "verification-email:user-123",
      "profile:user-123",
      "auth:user-123",
    ]);
  });

  it("preserves the account and purchase details if subscription cancellation fails", async () => {
    const repo = new FakeAccountDeletionRepository();
    repo.cancelActiveSubscription = async () => { throw new Error("Play unavailable"); };
    await expect(deleteAccount("user-123", repo)).rejects.toThrow("Play unavailable");
    expect(repo.calls).toEqual([]);
  });
});
