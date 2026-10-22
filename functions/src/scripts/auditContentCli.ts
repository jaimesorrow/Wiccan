import fs from "node:fs";
import { parseArgs } from "node:util";
import { getFirestore } from "firebase-admin/firestore";
import { auditPracticalContent } from "../practicalContentAudit";
import { loadContentSeed } from "../contentSeed";
import { loadSourcePassages } from "../sourcePassages";
import { initializeContentProject } from "./contentProject";

async function main(): Promise<void> {
  const { values } = parseArgs({ options: { project: { type: "string" }, output: { type: "string" } }, strict: true, allowPositionals: false });
  const passages = loadSourcePassages();
  let report;
  if (values.project) {
    initializeContentProject(values.project);
    const db = getFirestore();
    const [content, history] = await Promise.all([db.collection("content").get(), db.collection("userContentHistory").select("contentId").get()]);
    const deliveredIds = new Set(history.docs.map((doc) => doc.get("contentId")).filter((id): id is string => typeof id === "string"));
    report = {
      inventoryKind: "live-firestore", project: values.project, observedAt: new Date().toISOString(), historyRowsInspected: history.size,
      ...auditPracticalContent(content.docs.map((doc) => ({ id: doc.id, fields: doc.data(), updateTime: `${doc.updateTime.seconds}:${doc.updateTime.nanoseconds}` })), passages, deliveredIds),
    };
  } else {
    report = { inventoryKind: "checked-in-seed", liveCountVerified: false, liveTotals: null, ...auditPracticalContent(loadContentSeed().map(({ id, ...fields }) => ({ id, fields })), passages) };
  }
  const json = JSON.stringify(report, null, 2);
  if (values.output) fs.writeFileSync(values.output, json + "\n", { mode: 0o600, flag: "wx" });
  else console.log(json);
}
main().catch((error: unknown) => { console.error(error instanceof Error ? error.message : error); process.exitCode = 1; });
