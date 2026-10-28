import { createHash } from "node:crypto";
import fs from "node:fs";
import { getFirestore } from "firebase-admin/firestore";
import { parseArgs } from "node:util";
import { planDraftApproval } from "../approveDraft";
import type { DraftApprovalPlanEntry } from "../approveDraft";
import { FirestoreContentAdminRepository } from "../firestoreContentAdminRepository";
import { practicalContentHash, stableJson } from "../practicalLesson";
import { CONTENT_PROJECT_ID, initializeContentProject } from "./contentProject";

interface PublicationPlan {
  formatVersion: 1;
  projectId: typeof CONTENT_PROJECT_ID;
  operator: string;
  generatedAt: string;
  entries: DraftApprovalPlanEntry[];
}

function publicationPlanDigest(plan: PublicationPlan): string {
  return createHash("sha256").update(stableJson(plan)).digest("hex");
}

function readIds(values: { contentId?: string[]; batch?: string }): string[] {
  const fromFile = values.batch ? JSON.parse(fs.readFileSync(values.batch, "utf8")) as unknown : [];
  if (!Array.isArray(fromFile) || !fromFile.every((id) => typeof id === "string")) {
    throw new Error("--batch must contain a JSON array of document IDs.");
  }
  const ids = [...(values.contentId ?? []), ...fromFile].map((id) => id.trim());
  if (ids.length < 1 || ids.length > 25 || ids.some((id) => !id || id.includes("/")) || new Set(ids).size !== ids.length) {
    throw new Error("Supply 1 to 25 distinct document IDs using --contentId or --batch.");
  }
  return ids;
}

function validatePlan(value: unknown): PublicationPlan {
  if (!value || typeof value !== "object" || Array.isArray(value)) throw new Error("Publication plan must be an object.");
  const plan = value as Partial<PublicationPlan>;
  if (plan.formatVersion !== 1 || plan.projectId !== CONTENT_PROJECT_ID || typeof plan.operator !== "string" || !plan.operator.trim() ||
      typeof plan.generatedAt !== "string" || !Number.isFinite(Date.parse(plan.generatedAt)) || !Array.isArray(plan.entries) ||
      plan.entries.length < 1 || plan.entries.length > 25 || new Set(plan.entries.map((entry) => entry.contentId)).size !== plan.entries.length ||
      plan.entries.some((entry) => !entry.contentId || entry.contentId.includes("/") || !/^\d+:\d{1,9}$/.test(entry.expectedUpdateTime) || !/^[a-f0-9]{64}$/.test(entry.contentHash))) {
    throw new Error("Publication plan is malformed or targets another project.");
  }
  return plan as PublicationPlan;
}

async function main(): Promise<void> {
  const { values } = parseArgs({
    args: process.argv.slice(2),
    options: {
      project: { type: "string" }, contentId: { type: "string", multiple: true }, batch: { type: "string" },
      operator: { type: "string" }, plan: { type: "string" }, output: { type: "string" },
      commit: { type: "boolean", default: false }, approval: { type: "string" },
    },
    strict: true,
    allowPositionals: false,
  });
  initializeContentProject(values.project);
  const repo = new FirestoreContentAdminRepository(getFirestore());

  if (!values.commit) {
    if (!values.operator?.trim()) throw new Error("--operator is required for a dry-run publication plan.");
    if (values.plan || values.approval) throw new Error("--plan and --approval are commit-only options.");
    const ids = readIds(values);
    const entries: DraftApprovalPlanEntry[] = [];
    const blocked: { contentId: string; reason: string }[] = [];
    for (const contentId of ids) {
      try {
        entries.push(await planDraftApproval(contentId, repo));
      } catch (error) {
        blocked.push({ contentId, reason: error instanceof Error ? error.message : String(error) });
      }
    }
    const plan: PublicationPlan = {
      formatVersion: 1,
      projectId: CONTENT_PROJECT_ID,
      operator: values.operator.trim(),
      generatedAt: new Date().toISOString(),
      entries,
    };
    const report = { mode: "dry-run", writes: false, plan, planDigest: publicationPlanDigest(plan), blocked };
    const json = JSON.stringify(report, null, 2) + "\n";
    if (values.output) fs.writeFileSync(values.output, json, { mode: 0o600, flag: "wx" });
    else process.stdout.write(json);
    if (blocked.length > 0) process.exitCode = 1;
    return;
  }

  if (values.contentId || values.batch || values.operator || values.output) throw new Error("Commit accepts only --project, --commit, --plan and --approval.");
  if (!values.plan || !values.approval) throw new Error("--commit requires the reviewed --plan file and its exact --approval digest.");
  const envelope = JSON.parse(fs.readFileSync(values.plan, "utf8")) as { plan?: unknown };
  const plan = validatePlan(envelope.plan);
  const digest = publicationPlanDigest(plan);
  if (values.approval !== digest) throw new Error("Approval must match the exact publication plan digest.");

  for (const entry of plan.entries) {
    const current = await repo.getVersionedContent(entry.contentId);
    if (!current) throw new Error(`Content '${entry.contentId}' no longer exists.`);
    if (current.content.status === "published" && current.content.contentStatus === "published" && practicalContentHash(current.content) === entry.contentHash) continue;
    const refreshed = await planDraftApproval(entry.contentId, repo);
    if (stableJson(refreshed) !== stableJson(entry)) throw new Error(`Content '${entry.contentId}' changed after the dry run.`);
  }
  const results = await repo.publishBatch(plan.entries.map((entry) => ({
    contentId: entry.contentId,
    expectedUpdateTime: entry.expectedUpdateTime,
    expectedContentHash: entry.contentHash,
    operator: plan.operator,
    approvalDigest: digest,
  })));
  process.stdout.write(JSON.stringify({ mode: "approved-commit", projectId: CONTENT_PROJECT_ID, planDigest: digest, results }, null, 2) + "\n");
}

main().catch((error: unknown) => {
  console.error(error instanceof Error ? error.message : error);
  process.exitCode = 1;
});
