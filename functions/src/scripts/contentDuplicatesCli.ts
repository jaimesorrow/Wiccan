import { resolve } from "node:path";
import { checkDuplicates, IndexedLesson } from "../lessons/duplicates";
import { classifyPracticality, loadRepoInventory } from "../lessons/repoInventory";

/**
 * npm run content:duplicates [-- --legacy-audit]
 * Checks every canonical draft against the whole repository inventory (and
 * the drafts before it). Exits nonzero when a canonical draft duplicates
 * anything. --legacy-audit also prints the practical/informational audit and
 * the mapping issues for the legacy seed pool as JSON.
 */
function main(): void {
  const root = resolve(__dirname, "..", "..");
  const inventory = loadRepoInventory(root);
  inventory.sources.forEach((source) => console.log(`inventory ${source.name}: ${source.records} record(s)`));
  if (inventory.missing.length) {
    console.error(`Missing inventory sources: ${inventory.missing.join(", ")}`);
    process.exitCode = 1;
    return;
  }
  const drafts = inventory.lessons.filter((item) => item.source === "repo:lessons/drafts");
  const others = inventory.lessons.filter((item) => item.source !== "repo:lessons/drafts");
  let duplicates = 0;
  const seen: IndexedLesson[] = [...others];
  for (const draft of drafts) {
    // A rewrite legitimately replaces its own legacy record (same ID); compare it against everything else.
    const comparable = seen.filter((item) => item.lesson.id !== draft.lesson.id && item.lesson.id !== `${draft.lesson.id}#rewrite`);
    const report = checkDuplicates(draft, comparable);
    if (report.status !== "accepted-draft") {
      duplicates++;
      console.log(JSON.stringify(report, null, 2));
    }
    seen.push(draft);
  }
  console.log(`${drafts.length} canonical draft(s) checked against ${others.length} other record(s); ${duplicates} duplicate(s).`);

  if (process.argv.includes("--legacy-audit")) {
    const seed = inventory.lessons.filter((item) => item.source === "repo:ancientContentSeed.json");
    const issues = inventory.sources.find((s) => s.name === "repo:ancientContentSeed.json")?.mappingIssues ?? {};
    const audit = seed.map((item) => ({
      id: item.lesson.id,
      title: item.lesson.title,
      mappedCategory: item.lesson.primaryCategoryId,
      ...classifyPracticality(item.lesson),
      mappingIssues: issues[item.lesson.id] ?? [],
    }));
    console.log(JSON.stringify(audit, null, 2));
  }
  if (duplicates) process.exitCode = 1;
}

main();
