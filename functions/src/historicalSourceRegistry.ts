import fs from "node:fs";
import path from "node:path";
import { z } from "zod";

const webUrl = z.string().url().refine((value) => ["http:", "https:"].includes(new URL(value).protocol));

export const HistoricalSourceRecordSchema = z.object({
  id: z.string().trim().min(1),
  title: z.string().trim().min(1),
  status: z.enum(["approved", "rejected", "pending"]),
  sourceTradition: z.string().trim().min(1),
  historicalPeriod: z.string().trim().min(1),
  manuscriptOrEdition: z.string().trim().min(1),
  editorTranslator: z.string().trim().min(1),
  accessUrl: webUrl,
  rightsStatus: z.enum(["verified", "pending", "blocked"]),
  rightsEvidenceUrl: webUrl,
  rightsNotes: z.string().trim().min(1),
  passageStatus: z.enum(["approved", "rejected", "pending"]),
  limitations: z.array(z.string().trim().min(1)).min(1),
}).superRefine((record, context) => {
  if (record.status === "approved" && (record.rightsStatus !== "verified" || record.passageStatus !== "approved")) {
    context.addIssue({ code: "custom", message: "Approved sources require verified rights and at least one approved passage." });
  }
});

export type HistoricalSourceRecord = z.infer<typeof HistoricalSourceRecordSchema>;

export function loadHistoricalSourceRegistry(
  registryPath = path.join(__dirname, "..", "historicalSourceRegistry.json"),
): HistoricalSourceRecord[] {
  const value: unknown = JSON.parse(fs.readFileSync(registryPath, "utf8"));
  const records = z.array(HistoricalSourceRecordSchema).parse(value);
  if (new Set(records.map((record) => record.id)).size !== records.length) throw new Error("Historical source IDs must be unique.");
  return records;
}
