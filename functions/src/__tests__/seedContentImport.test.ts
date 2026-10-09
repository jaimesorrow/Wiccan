import { Firestore } from "firebase-admin/firestore";
import { loadContentSeed } from "../contentSeed";
import { createMissingSeedContent, findSeedImportConflicts } from "../seedContentImport";

type Fields = Record<string, unknown>;
interface Ref { id: string }

/** Atomic transaction fake; failed/retried callbacks discard their queued writes. */
function fakeFirestore(initial: Record<string, Fields> = {}, retryWith?: () => Record<string, Fields>) {
  const stored = new Map(Object.entries(initial));
  let retryPending = retryWith !== undefined;
  const db = {
    collection: () => ({ doc: (id: string): Ref => ({ id }) }),
    runTransaction: async <T>(callback: (transaction: {
      getAll: (...refs: Ref[]) => Promise<{ exists: boolean; get: (field: string) => unknown }[]>;
      create: (ref: Ref, fields: Fields) => void;
      set: (ref: Ref, fields: Fields, options: { merge: true }) => void;
    }) => Promise<T>): Promise<T> => {
      const execute = async () => {
        const queued = new Map<string, Fields>();
        const merged = new Map<string, Fields>();
        const result = await callback({
          getAll: async (...refs) => refs.map(({ id }) => ({
            exists: stored.has(id),
            get: (field) => stored.get(id)?.[field],
          })),
          create: ({ id }, fields) => queued.set(id, fields),
          set: ({ id }, fields) => merged.set(id, fields),
        });
        return { result, queued, merged };
      };
      let completed = await execute();
      if (retryPending && retryWith) {
        retryPending = false;
        for (const [id, fields] of Object.entries(retryWith())) stored.set(id, fields);
        completed = await execute();
      }
      for (const [id, fields] of completed.queued) {
        if (stored.has(id)) throw new Error("create precondition failed");
        stored.set(id, fields);
      }
      for (const [id, fields] of completed.merged) {
        const current = { ...stored.get(id) };
        // FieldValue.delete() is deliberately interpreted by this focused fake.
        if (Object.hasOwn(fields, "difficulty")) delete current.difficulty;
        stored.set(id, { ...current, similarityGroupId: fields.similarityGroupId });
      }
      return completed.result;
    },
  };
  return { db: db as unknown as Firestore, stored };
}

describe("safe content seed import", () => {
  const entries = loadContentSeed().slice(0, 3);

  it("creates only missing drafts and is idempotent", async () => {
    const { db, stored } = fakeFirestore();
    const first = await createMissingSeedContent(db, entries);
    expect(first.createdIds).toEqual(entries.map((entry) => entry.id));
    for (const fields of stored.values()) expect(fields.status).toBe("draft");
    const second = await createMissingSeedContent(db, entries);
    expect(second.createdIds).toEqual([]);
    expect(second.preservedIds).toHaveLength(3);
    expect(second.metadataSyncedIds).toHaveLength(3);
    expect(stored.size).toBe(3);
  });

  it("preserves every field of an existing published document and an edited draft", async () => {
    const published = {
      ...entries[0],
      status: "published",
      body: "Editor-approved production body",
      editorialRevision: 17,
      difficulty: "advanced",
      similarityGroupId: "stale-group",
    };
    const draft = { ...entries[1], body: "Draft under review", customMetadata: true };
    const { db, stored } = fakeFirestore({ [entries[0].id]: published, [entries[1].id]: draft });
    const result = await createMissingSeedContent(db, entries);
    expect(result.createdIds).toEqual([entries[2].id]);
    expect(result.preservedModifiedIds).toEqual([entries[0].id, entries[1].id]);
    expect(stored.get(entries[0].id)).toEqual({
      ...published,
      difficulty: undefined,
      similarityGroupId: entries[0].similarityGroupId,
    });
    expect(stored.get(entries[1].id)).toEqual(draft);
  });

  it.each(["type", "sourceId"])("aborts the transaction on incompatible %s at a seed ID", async (field) => {
    const collision = { ...entries[1], [field]: "incompatible" };
    const { db, stored } = fakeFirestore({ [entries[1].id]: collision });
    await expect(createMissingSeedContent(db, entries)).rejects.toThrow("collides");
    expect(stored.size).toBe(1);
    expect(stored.get(entries[1].id)).toEqual(collision);
  });

  it("re-reads concurrent publication on a transaction retry and does not count discarded writes", async () => {
    const published = { ...entries[0], status: "published", body: "Published while import retried" };
    const { db, stored } = fakeFirestore({}, () => ({ [entries[0].id]: published }));
    const result = await createMissingSeedContent(db, entries);
    expect(result.createdIds).toEqual([entries[1].id, entries[2].id]);
    expect(result.preservedIds).toEqual([entries[0].id]);
    expect(stored.get(entries[0].id)).toEqual(published);
  });

  it("detects a missing seed ID duplicating a different live ID before writes", () => {
    const titleTwin = { id: "other-id", fields: { title: ` ${entries[0].title.toUpperCase()} `, body: "Different body" } };
    expect(findSeedImportConflicts([titleTwin], entries).join(" ")).toContain("duplicates the title or body");
  });

  it("allows a compatible edited document at the same ID to stay preserved", () => {
    const existing = { id: entries[0].id, fields: { ...entries[0], body: "Edited production text" } };
    expect(findSeedImportConflicts([existing], entries)).toEqual([]);
  });
});
