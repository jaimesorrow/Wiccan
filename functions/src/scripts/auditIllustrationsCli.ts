import fs from "node:fs";
import { parseArgs } from "node:util";
import { getFirestore } from "firebase-admin/firestore";
import { auditIllustrations } from "../illustrationAudit";
import { loadContentSeed } from "../contentSeed";
import { initializeContentProject } from "./contentProject";

async function main(): Promise<void> {
  const { values } = parseArgs({
    options: { project: { type: "string" }, output: { type: "string" } },
    strict: true,
    allowPositionals: false,
  });
  let inventoryKind = "checked-in-seed";
  let documents;
  if (values.project) {
    initializeContentProject(values.project);
    const snapshot = await getFirestore().collection("content").get();
    inventoryKind = "live-firestore";
    documents = snapshot.docs.map((document) => ({ id: document.id, fields: document.data() }));
  } else {
    documents = loadContentSeed().map(({ id, ...fields }) => ({ id, fields }));
  }
  const report = {
    inventoryKind,
    project: values.project ?? null,
    observedAt: new Date().toISOString(),
    mutatesData: false,
    ...auditIllustrations(documents),
  };
  const json = JSON.stringify(report, null, 2) + "\n";
  if (values.output) fs.writeFileSync(values.output, json, { mode: 0o600, flag: "wx" });
  else process.stdout.write(json);
  if (report.totals.invalidPublished > 0) process.exitCode = 1;
}

main().catch((error: unknown) => {
  console.error(error instanceof Error ? error.message : error);
  process.exitCode = 1;
});
