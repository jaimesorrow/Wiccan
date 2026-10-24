import { initializeApp } from "firebase-admin/app";
import { GeoPoint, Timestamp, getFirestore } from "firebase-admin/firestore";
import {
  contentBackupSha256,
  decodeFirestoreValue,
  encodeFirestoreValue,
  parseContentBackup,
  serializeContentBackup,
} from "../contentBackup";
import type { ContentBackupArtifact } from "../contentBackup";

describe("root content backup format", () => {
  const db = getFirestore(initializeApp({ projectId: "demo-backup-unit" }, "backup-unit"));

  it("round-trips Firestore field types without flattening timestamps or references", () => {
    const original = {
      title: "Lesson",
      createdAt: new Timestamp(1_700_000_000, 123_000_000),
      point: new GeoPoint(12.5, -33.25),
      bytes: Buffer.from("wiccan"),
      reference: db.doc("sources/example"),
      nested: { values: [true, null, Number.NaN] },
    };
    const decoded = decodeFirestoreValue(encodeFirestoreValue(original), db) as typeof original;

    expect(encodeFirestoreValue(decoded)).toEqual(encodeFirestoreValue(original));
  });

  it("serializes a deterministic, validated gzip artifact", () => {
    const artifact: ContentBackupArtifact = {
      formatVersion: 1,
      projectId: "wiccan-android",
      databaseId: "(default)",
      collectionPath: "content",
      completedAt: "2026-10-23T00:00:00.000Z",
      documents: [{
        id: "lesson-1",
        createTime: "2026-01-01T00:00:00.000Z",
        updateTime: "1:2",
        data: encodeFirestoreValue({ title: "Lesson", status: "draft" }),
      }],
    };
    const bytes = serializeContentBackup(artifact);

    expect(parseContentBackup(bytes)).toEqual(artifact);
    expect(contentBackupSha256(bytes)).toMatch(/^[a-f0-9]{64}$/);
  });

  it("rejects duplicate document IDs", () => {
    const artifact: ContentBackupArtifact = {
      formatVersion: 1,
      projectId: "wiccan-android",
      databaseId: "(default)",
      collectionPath: "content",
      completedAt: "2026-10-23T00:00:00.000Z",
      documents: ["one", "two"].map(() => ({
        id: "duplicate",
        createTime: "2026-01-01T00:00:00.000Z",
        updateTime: "1:2",
        data: encodeFirestoreValue({ status: "draft" }),
      })),
    };

    expect(() => serializeContentBackup(artifact)).toThrow("invalid or duplicate document");
  });
});
