import { getFirestore } from "firebase-admin/firestore";
import { parseArgs } from "node:util";
import { loadContentSources } from "../contentSource";
import { ClaudeDraftGenerator } from "../claudeDraftGenerator";
import { FirestoreContentAdminRepository } from "../firestoreContentAdminRepository";
import { generateDrafts } from "../generateDraft";
import { ContentTier, ContentType } from "../types";
import { CONTENT_PROJECT_ID, initializeContentProject } from "./contentProject";

/**
 * Admin CLI: generates content drafts via Claude. Run locally by a trusted
 * operator authenticated as a service account (GOOGLE_APPLICATION_CREDENTIALS
 * pointing at a key file) — never deployed as a public Cloud Function, per
 * Session 17's lockdown of content generation/approval.
 *
 * Preview (no API call or write):
 *   npm run content-agent -- --tier free [--sourceId <id>] [--type spell] [--count 3]
 * Commit drafts to production:
 *   npm run content-agent -- --project wiccan-android --commit --tier free [...]
 */

const VALID_TIERS: ReadonlySet<string> = new Set(["free", "premium"]);
const VALID_TYPES: ReadonlySet<string> = new Set(["spell", "potion", "lesson"]);

async function main(): Promise<void> {
  const { values: args } = parseArgs({
    args: process.argv.slice(2),
    options: {
      project: { type: "string" },
      commit: { type: "boolean", default: false },
      tier: { type: "string" },
      sourceId: { type: "string" },
      type: { type: "string" },
      count: { type: "string" },
      similarityGroupId: { type: "string" },
      sourceCitation: { type: "string" },
    },
    strict: true,
    allowPositionals: false,
  });

  const tier = args.tier;
  if (!tier || !VALID_TIERS.has(tier)) {
    throw new Error(`Usage: --tier <free|premium> [--sourceId <id>] [--type <spell|potion|lesson>] [--count <n>] [--project ${CONTENT_PROJECT_ID} --commit]`);
  }
  if (args.type !== undefined && !VALID_TYPES.has(args.type)) {
    throw new Error("--type must be 'spell', 'potion', or 'lesson' when provided.");
  }
  const count = args.count !== undefined ? Number(args.count) : undefined;
  if (count !== undefined && (!Number.isInteger(count) || count < 1)) {
    throw new Error("--count must be a positive integer when provided.");
  }
  if (args.project !== undefined && args.project !== CONTENT_PROJECT_ID) {
    throw new Error(`This content workflow only targets ${CONTENT_PROJECT_ID}; received '${args.project}'.`);
  }
  if (args.commit && args.project !== CONTENT_PROJECT_ID) {
    throw new Error(`Explicit --project ${CONTENT_PROJECT_ID} is required when --commit is used.`);
  }

  const sources = loadContentSources();
  const eligibleSources = sources.filter((source) =>
    source.era === "ancient" &&
    source.sourceKind === "ancient-grimoire" &&
    (args.sourceId === undefined || source.id === args.sourceId) &&
    (args.type === undefined || source.eligibleTypes.includes(args.type as ContentType))
  );
  if (args.sourceId !== undefined && !sources.some((source) => source.id === args.sourceId)) {
    throw new Error(`Unknown sourceId '${args.sourceId}'.`);
  }
  if (eligibleSources.length === 0) {
    throw new Error("No approved ancient grimoire supports the requested source/type selection.");
  }

  if (!args.commit) {
    console.log(JSON.stringify({
      mode: "preview",
      writes: false,
      count: count ?? 1,
      tier,
      type: args.type ?? "all eligible types",
      eligibleSourceIds: eligibleSources.map((source) => source.id),
      nextStep: `Add --project ${CONTENT_PROJECT_ID} --commit to call Claude and create review-required drafts.`,
    }, null, 2));
    return;
  }

  if (!process.env.ANTHROPIC_API_KEY?.trim()) {
    throw new Error("ANTHROPIC_API_KEY is required when --commit is used.");
  }
  initializeContentProject(args.project);
  console.log(`Operating against Firebase project: ${CONTENT_PROJECT_ID}`);

  const generator = new ClaudeDraftGenerator();
  const repo = new FirestoreContentAdminRepository(getFirestore());

  const results = await generateDrafts(
    {
      tier: tier as ContentTier,
      sourceId: args.sourceId,
      type: args.type as ContentType | undefined,
      count,
      similarityGroupId: args.similarityGroupId,
      sourceCitation: args.sourceCitation,
    },
    sources,
    generator,
    repo,
  );

  console.log(JSON.stringify(results, null, 2));
}

main().catch((err) => {
  console.error(err instanceof Error ? err.message : err);
  process.exitCode = 1;
});
