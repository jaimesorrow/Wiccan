import fs from "node:fs";
import { parseArgs } from "node:util";
import { getFirestore } from "firebase-admin/firestore";
import { GoogleAuth } from "google-auth-library";
import { summarizeContentInventory } from "../contentInventoryPreflight";
import { initializeContentProject } from "./contentProject";

const TARGET_PROJECT = "wiccan-android";
const REPORT_TIME_ZONE = "America/Anchorage";

async function main(): Promise<void> {
  const { values } = parseArgs({
    options: {
      project: { type: "string" },
      output: { type: "string" },
    },
    strict: true,
    allowPositionals: false,
  });
  if (values.project !== TARGET_PROJECT) {
    throw new Error(`--project must be exactly ${TARGET_PROJECT}.`);
  }

  // Fail before Firestore opens a gRPC stream when this environment has no
  // authorized Application Default Credentials. This keeps the blocker clear
  // and prevents a background credential lookup from producing a stack trace.
  await new GoogleAuth({ projectId: values.project }).getClient();
  initializeContentProject(values.project);
  const db = getFirestore();
  // Keep the two reads sequential so an authentication failure is reported
  // once through the CLI's normal error boundary instead of leaving a second
  // in-flight request to reject after the process has begun shutting down.
  const content = await db.collection("content").select("type", "status").get();
  const disclosures = await db.collection("settings").doc("disclosures").get();
  const observedAt = new Date();
  const report = {
    inventoryKind: "live-firestore-read-only",
    project: values.project,
    observedAt: observedAt.toISOString(),
    observedAtAnchorage: new Intl.DateTimeFormat("en-US", {
      timeZone: REPORT_TIME_ZONE,
      dateStyle: "full",
      timeStyle: "long",
    }).format(observedAt),
    timeZone: REPORT_TIME_ZONE,
    content: summarizeContentInventory(content.docs.map((doc) => ({
      type: doc.get("type"),
      status: doc.get("status"),
    }))),
    disclosure: {
      documentExists: disclosures.exists,
      potionSafetyDisclosureExists: typeof disclosures.get("potionSafetyDisclosure") === "string" &&
        disclosures.get("potionSafetyDisclosure").trim().length > 0,
    },
  };
  const json = JSON.stringify(report, null, 2);
  if (values.output) fs.writeFileSync(values.output, json + "\n", { mode: 0o600, flag: "wx" });
  else console.log(json);
}

main().catch((error: unknown) => {
  const message = error instanceof Error ? error.message : String(error);
  console.error(
    message.includes("Could not load the default credentials")
      ? "No authorized Google Application Default Credentials are available; the live Firestore inventory was not read."
      : message,
  );
  process.exitCode = 1;
});

