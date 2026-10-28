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
    const collectionNames = [
      "content", "userContentHistory", "userContentUsage", "users", "subscriptions", "purchaseTokens",
      "userPurchaseTokens", "contentPoolExhaustionEvents", "contentPublicationAudit", "settings", "systemConfig",
    ] as const;
    const [content, history, counts, usageContentGroup, deviceCount, userTokenCount, similarityCount, disclosures, globalConfig] = await Promise.all([
      db.collection("content").get(),
      db.collection("userContentHistory").select("contentId").get(),
      Promise.all(collectionNames.map(async (name) => [name, (await db.collection(name).count().get()).data().count] as const)),
      db.collectionGroup("content").select().get(),
      db.collectionGroup("devices").count().get(),
      db.collectionGroup("tokens").count().get(),
      db.collectionGroup("similarityGroups").count().get(),
      db.collection("settings").doc("disclosures").get(),
      db.collection("systemConfig").doc("global").get(),
    ]);
    const deliveredIds = new Set(history.docs.map((doc) => doc.get("contentId")).filter((id): id is string => typeof id === "string"));
    report = {
      inventoryKind: "live-firestore", project: values.project, observedAt: new Date().toISOString(), historyRowsInspected: history.size,
      collectionInventory: {
        ...Object.fromEntries(counts),
        "userContentUsage/{uid}/content": usageContentGroup.docs.filter((doc) => doc.ref.parent.parent !== null).length,
        "users/{uid}/devices": deviceCount.data().count,
        "userPurchaseTokens/{uid}/tokens": userTokenCount.data().count,
        "userContentUsage/{uid}/similarityGroups": similarityCount.data().count,
      },
      requiredConfiguration: {
        disclosuresDocumentExists: disclosures.exists,
        potionSafetyDisclosureConfigured: typeof disclosures.get("potionSafetyDisclosure") === "string" && disclosures.get("potionSafetyDisclosure").trim().length > 0,
        globalConfigDocumentExists: globalConfig.exists,
        dailyContentEnabled: globalConfig.exists ? globalConfig.get("dailyContentEnabled") !== false : "default-enabled",
        notificationsEnabled: globalConfig.exists ? globalConfig.get("notificationsEnabled") !== false : "default-enabled",
      },
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
