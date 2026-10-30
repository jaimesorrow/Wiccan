import fs from "node:fs";
import path from "node:path";
import { parseArgs } from "node:util";
import { loadContentSeed } from "../contentSeed";
import { createRewriteStagingBatches, DEFAULT_REWRITE_BATCH_SIZE } from "../rewriteBatchStaging";

async function main(): Promise<void> {
  const { values } = parseArgs({
    options: {
      output: { type: "string" },
      seed: { type: "string" },
      "batch-size": { type: "string", default: String(DEFAULT_REWRITE_BATCH_SIZE) },
    },
    strict: true,
    allowPositionals: false,
  });
  if (!values.output?.trim()) throw new Error("--output must name a new local staging directory.");
  const batchSize = Number(values["batch-size"]);
  const batches = createRewriteStagingBatches(loadContentSeed(values.seed), batchSize);
  const output = path.resolve(values.output);
  fs.mkdirSync(output, { mode: 0o700 });
  const index = {
    schemaVersion: 1,
    projectId: "wiccan-android",
    sourceKind: "checked-in-seed",
    writes: false,
    liveBindingRequired: true,
    totalRecords: batches.reduce((total, batch) => total + batch.entries.length, 0),
    totalBatches: batches.length,
    requestedBatchSize: batchSize,
    batches: batches.map((batch) => ({
      batchNumber: batch.batchNumber,
      file: `batch-${String(batch.batchNumber).padStart(2, "0")}.json`,
      recordCount: batch.entries.length,
      batchSha256: batch.batchSha256,
    })),
  };
  for (const batch of batches) {
    const file = `batch-${String(batch.batchNumber).padStart(2, "0")}.json`;
    fs.writeFileSync(path.join(output, file), JSON.stringify(batch, null, 2) + "\n", { mode: 0o600, flag: "wx" });
  }
  fs.writeFileSync(path.join(output, "index.json"), JSON.stringify(index, null, 2) + "\n", { mode: 0o600, flag: "wx" });
  process.stdout.write(JSON.stringify(index, null, 2) + "\n");
}

main().catch((error: unknown) => {
  console.error(error instanceof Error ? error.message : error);
  process.exitCode = 1;
});
