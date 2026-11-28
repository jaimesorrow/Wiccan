import { Firestore } from "firebase-admin/firestore";
import { FirestoreNotificationUserRepository } from "../firestoreNotificationUserRepository";

function deviceDoc(uid: string, data: Record<string, unknown>) {
  return {
    get: (field: string) => data[field],
    ref: { parent: { parent: { id: uid } } },
  };
}

describe("FirestoreNotificationUserRepository", () => {
  it("defaults a legacy device with no notificationsEnabled field to enabled", async () => {
    const db = {
      collectionGroup: () => ({
        get: async () => ({
          docs: [deviceDoc("legacy-user", { token: "legacy-token", timeZone: "Etc/UTC" })],
        }),
      }),
    } as unknown as Firestore;

    await expect(new FirestoreNotificationUserRepository(db).listDevicesWithToken()).resolves.toEqual([
      {
        uid: "legacy-user",
        token: "legacy-token",
        timeZone: "Etc/UTC",
        notificationsEnabled: true,
        locale: "en",
      },
    ]);
  });
});
