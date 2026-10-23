import fs from "node:fs";
import { parseArgs } from "node:util";
import { compareContentAudits } from "../contentAuditComparison";
import type { ComparableAudit } from "../contentAuditComparison";
import type { ReviewedChangeSet } from "../reviewedContentChanges";

function readJson<T>(path: string): T {
  return JSON.parse(fs.readFileSync(path, "utf8")) as T;
}

function main(): void {
  const { values } = parseArgs({ options: {
    before: { type: "string" }, after: { type: "string" },
    changeset: { type: "string" }, output: { type: "string" },
  }, strict: true, allowPositionals: false });
  if (!values.before || !values.after) throw new Error("--before and --after live audit files are required.");
  const expectedIds = values.changeset
    ? new Set(readJson<ReviewedChangeSet>(values.changeset).entries.map((entry) => entry.id))
    : new Set<string>();
  const report = compareContentAudits(readJson<ComparableAudit>(values.before), readJson<ComparableAudit>(values.after), expectedIds);
  const json = JSON.stringify(report, null, 2) + "\n";
  if (values.output) fs.writeFileSync(values.output, json, { mode: 0o600, flag: "wx" });
  else process.stdout.write(json);
  if (!report.passed) process.exitCode = 1;
}

try { main(); } catch (error: unknown) {
  console.error(error instanceof Error ? error.message : error);
  process.exitCode = 1;
}
