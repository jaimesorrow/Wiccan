import fs from "node:fs";
import { parseArgs } from "node:util";
import { FieldPath, getFirestore } from "firebase-admin/firestore";
import { approveDraft, validateDraftForPublication } from "../approveDraft";
import { FirestoreContentAdminRepository } from "../firestoreContentAdminRepository";
import type { StoredContent } from "../contentAdminRepositoryPort";
import { CONTENT_PROJECT_ID, initializeContentProject } from "./contentProject";

interface PublicationResult {
  id: string;
  outcome: "would-publish" | "published" | "blocked" | "failed";
  reasons?: string[];
  attempts?: number;
}

const delay = async (attempt: number): Promise<void> =>
  new Promise((resolve) => setTimeout(resolve, Math.min(250 * 2 ** attempt, 2_000)));

async function main(): Promise<void> {
  const { values } = parseArgs({ options: {
    project: { type: "string" }, commit: { type: "boolean", default: false },
    output: { type: "string" }, batchSize: { type: "string", default: "100" },
    maxRetries: { type: "string", default: "3" },
  }, strict: true, allowPositionals: false });
  if (values.project !== CONTENT_PROJECT_ID) throw new Error(`Explicit --project ${CONTENT_PROJECT_ID} is required.`);
  if (!values.output) throw new Error("--output is required.");
  const batchSize = Number(values.batchSize);
  const maxRetries = Number(values.maxRetries);
  if (!Number.isInteger(batchSize) || batchSize < 1 || batchSize > 400) throw new Error("--batchSize must be an integer from 1 to 400.");
  if (!Number.isInteger(maxRetries) || maxRetries < 0 || maxRetries > 8) throw new Error("--maxRetries must be an integer from 0 to 8.");

  initializeContentProject(values.project);
  const db = getFirestore();
  const repo = new FirestoreContentAdminRepository(db);
  const results: PublicationResult[] = [];
  let cursor: FirebaseFirestore.QueryDocumentSnapshot | undefined;
  do {
    let query = db.collection("content").where("status", "==", "draft")
      .orderBy(FieldPath.documentId()).limit(batchSize);
    if (cursor) query = query.startAfter(cursor);
    const snapshot = await query.get();
    for (const document of snapshot.docs) {
      const content = document.data() as StoredContent;
      const reasons = validateDraftForPublication(content);
      if (reasons.length) {
        results.push({ id: document.id, outcome: "blocked", reasons });
        continue;
      }
      if (!values.commit) {
        results.push({ id: document.id, outcome: "would-publish" });
        continue;
      }
      let failure: unknown;
      for (let attempt = 0; attempt <= maxRetries; attempt += 1) {
        try {
          // Re-reads and revalidates on every attempt; publish itself is an
          // idempotent transaction, so transient retry cannot double-publish.
          await approveDraft(document.id, repo);
          results.push({ id: document.id, outcome: "published", attempts: attempt + 1 });
          failure = undefined;
          break;
        } catch (error) {
          failure = error;
          if (attempt < maxRetries) await delay(attempt);
        }
      }
      if (failure) results.push({
        id: document.id, outcome: "failed", attempts: maxRetries + 1,
        reasons: [failure instanceof Error ? failure.message : String(failure)],
      });
    }
    cursor = snapshot.docs[snapshot.docs.length - 1];
    if (snapshot.size < batchSize) break;
  } while (cursor);

  const counts = results.reduce<Record<string, number>>((totals, result) => {
    totals[result.outcome] = (totals[result.outcome] ?? 0) + 1;
    return totals;
  }, {});
  const report = { projectId: CONTENT_PROJECT_ID, mode: values.commit ? "commit" : "dry-run", generatedAt: new Date().toISOString(), counts, results };
  fs.writeFileSync(values.output, JSON.stringify(report, null, 2) + "\n", { mode: 0o600, flag: "wx" });
  if (results.some((result) => result.outcome === "failed")) process.exitCode = 1;
}

main().catch((error: unknown) => {
  console.error(error instanceof Error ? error.message : error);
  process.exitCode = 1;
});
