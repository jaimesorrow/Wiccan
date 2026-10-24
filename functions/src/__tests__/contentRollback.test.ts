import { encodeFirestoreValue } from "../contentBackup";
import type { ContentBackupArtifact } from "../contentBackup";
import { buildContentRollbackPlan, contentRollbackPlanDigest } from "../contentRollback";
import type { PostMigrationAudit } from "../contentRollback";
import type { ReviewedChangeSet } from "../reviewedContentChanges";

const artifact: ContentBackupArtifact = {
  formatVersion: 1,
  projectId: "wiccan-android",
  databaseId: "(default)",
  collectionPath: "content",
  completedAt: "2026-10-23T00:00:00.000Z",
  documents: [{
    id: "lesson-1", createTime: "2026-01-01T00:00:00.000Z", updateTime: "1:1",
    data: encodeFirestoreValue({ status: "draft", title: "Before" }),
  }],
};
const changeSet: ReviewedChangeSet = {
  projectId: "wiccan-android", reviewId: "review-1",
  entries: [{ id: "lesson-1", expectedUpdateTime: "1:1", fields: { title: "After" } }],
};
const afterAudit: PostMigrationAudit = {
  inventoryKind: "live-firestore", project: "wiccan-android",
  records: [{ id: "lesson-1", updateTime: "2:2" }],
};

describe("content rollback plan", () => {
  it("binds rollback to the backup hash and post-migration update times", () => {
    const plan = buildContentRollbackPlan(artifact, "a".repeat(64), afterAudit, changeSet);

    expect(plan.entries).toEqual([{ id: "lesson-1", expectedUpdateTime: "2:2" }]);
    expect(contentRollbackPlanDigest(plan)).toMatch(/^[a-f0-9]{64}$/);
  });

  it("rejects a changed document that is absent from the backup", () => {
    const unknownChangeSet = { ...changeSet, entries: [{ ...changeSet.entries[0], id: "missing" }] };

    expect(() => buildContentRollbackPlan(artifact, "a".repeat(64), afterAudit, unknownChangeSet))
      .toThrow("Backup does not contain 'missing'");
  });
});
