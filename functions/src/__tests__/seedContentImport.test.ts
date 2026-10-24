import { Firestore } from "firebase-admin/firestore";
import { loadContentSeed } from "../contentSeed";
import { createMissingSeedContent, findSeedImportConflicts } from "../seedContentImport";

function emptyFirestore() {
  const stored = new Map<string, Record<string, unknown>>();
  const db = {
    collection: () => ({ doc: (id: string) => ({ id }) }),
    runTransaction: async <T>(callback: (transaction: {
      getAll: (...refs: { id: string }[]) => Promise<{ exists: boolean; get: (field: string) => unknown }[]>;
      create: (ref: { id: string }, fields: Record<string, unknown>) => void;
    }) => Promise<T>) => callback({
      getAll: async (...refs) => refs.map(() => ({ exists: false, get: () => undefined })),
      create: ({ id }, fields) => stored.set(id, fields),
    }),
  };
  return { db: db as unknown as Firestore, stored };
}

describe("safe content seed import", () => {
  const entries = loadContentSeed().slice(0, 3);

  it("blocks the legacy seed until passage and practical reviews exist", async () => {
    const { db, stored } = emptyFirestore();
    await expect(createMissingSeedContent(db, entries)).rejects.toThrow("needs passage/editorial review");
    expect(stored.size).toBe(0);
  });

  it("detects a missing seed ID duplicating a different live ID before writes", () => {
    const twin = { id: "other-id", fields: { title: ` ${entries[0].title.toUpperCase()} `, body: "Different body" } };
    expect(findSeedImportConflicts([twin], entries).join(" ")).toContain("duplicates the title or body");
  });

  it("allows a compatible edited document at the same ID to stay preserved", () => {
    const existing = { id: entries[0].id, fields: { ...entries[0], body: "Edited production text" } };
    expect(findSeedImportConflicts([existing], entries)).toEqual([]);
  });
});
