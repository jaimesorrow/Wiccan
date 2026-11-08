import { resolve } from "node:path";
import { validateLesson } from "../lessons/validateLesson";
import { readCanonicalDrafts } from "../lessons/repoInventory";

/**
 * npm run content:validate — validates every canonical draft in
 * functions/lessons/drafts. Exits nonzero on any schema, safety or source issue.
 */
function main(): void {
  const root = resolve(__dirname, "..", "..");
  const drafts = readCanonicalDrafts(root);
  let failures = 0;
  for (const lesson of drafts) {
    const issues = validateLesson(lesson);
    if (issues.length) {
      failures++;
      console.log(`FAIL ${lesson.id}`);
      issues.forEach((issue) => console.log(`  [${issue.code}] ${issue.field}: ${issue.message}`));
    }
  }
  console.log(`${drafts.length} canonical draft(s) checked, ${failures} failing.`);
  if (failures) process.exitCode = 1;
}

main();
