import { getApp, initializeApp } from "firebase-admin/app";
import { getFirestore } from "firebase-admin/firestore";
import { loadContentSources } from "../contentSource";
import { ClaudeDraftGenerator } from "../claudeDraftGenerator";
import { FirestoreContentAdminRepository } from "../firestoreContentAdminRepository";
import { generateDrafts } from "../generateDraft";
import { ContentTier, ContentType } from "../types";

/**
 * Admin CLI: generates content drafts via Claude. Run locally by a trusted
 * operator authenticated as a service account (GOOGLE_APPLICATION_CREDENTIALS
 * pointing at a key file) — never deployed as a public Cloud Function, per
 * Session 17's lockdown of content generation/approval.
 *
 * Usage: npm run generate-draft -- --tier free [--sourceId <id>] [--type spell] [--count 3]
 *        [--similarityGroupId <existing-lesson-group>] [--sourceCitation <passage>]
 */

const VALID_TIERS: ReadonlySet<string> = new Set(["free", "premium"]);
const VALID_TYPES: ReadonlySet<string> = new Set(["spell", "potion", "lesson"]);

function parseArgs(argv: string[]): Record<string, string> {
  const args: Record<string, string> = {};
  for (let i = 0; i < argv.length; i += 2) {
    const key = argv[i];
    const value = argv[i + 1];
    if (!key?.startsWith("--") || value === undefined) {
      throw new Error(`Malformed argument near '${argv[i]}'.`);
    }
    args[key.slice(2)] = value;
  }
  return args;
}

async function main(): Promise<void> {
  const args = parseArgs(process.argv.slice(2));

  const tier = args.tier;
  if (!tier || !VALID_TIERS.has(tier)) {
    throw new Error("Usage: --tier <free|premium> [--sourceId <id>] [--type <spell|potion|lesson>] [--count <n>] [--similarityGroupId <group>] [--sourceCitation <passage>]");
  }
  if (args.type !== undefined && !VALID_TYPES.has(args.type)) {
    throw new Error("--type must be 'spell', 'potion', or 'lesson' when provided.");
  }
  const count = args.count !== undefined ? Number(args.count) : undefined;
  if (count !== undefined && (!Number.isInteger(count) || count < 1)) {
    throw new Error("--count must be a positive integer when provided.");
  }

  initializeApp();
  // Printed first and unconditionally: this writes drafts into a real
  // project's content pool, so the operator gets one last look at which
  // environment they're about to touch before anything happens.
  console.log(`Operating against Firebase project: ${getApp().options.projectId ?? "(unknown)"}`);

  const sources = loadContentSources();
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
  console.error(err);
  process.exit(1);
});
