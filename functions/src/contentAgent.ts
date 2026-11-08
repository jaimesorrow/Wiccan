import { findPassageViolations } from "./sourcePassages";
import type { SourcePassage } from "./sourcePassages";
import { ContentSource, isLessonGenerationSource } from "./contentSource";
import { ContentTier } from "./types";
import { DraftGenerationPort } from "./draftGenerationPort";
import { ContentAdminRepositoryPort } from "./contentAdminRepositoryPort";
import { generateDraftBatch } from "./generateDraft";
import type { GeneratedDraftBatchResult } from "./generateDraft";

export const CONTENT_AGENT_TIERS: readonly ContentTier[] = ["free", "premium"];
export const DEFAULT_CONTENT_AGENT_BATCH_SIZE = 3;
/** generateDraftBatch makes up to 5 attempts per draft, so 5 caps a run at 25 model calls. */
export const MAX_CONTENT_AGENT_BATCH_SIZE = 5;

export interface ContentAgentSettings {
  enabled: boolean;
  batchSize: number;
}

export type ContentAgentRunStatus = "disabled" | "no-verified-passages" | "completed" | "exhausted" | "failed";

export interface ContentAgentRunReport {
  status: ContentAgentRunStatus;
  startedAt: string;
  cursor: number;
  passageId: string | null;
  sourceId: string | null;
  tier: ContentTier | null;
  requested: number;
  accepted: Array<{ contentId: string; title: string; type: string }>;
  rejected: GeneratedDraftBatchResult["rejected"];
  attempts: number;
  error: string | null;
  /** Always false: the agent only ever creates review-required drafts. */
  published: false;
}

/** Scheduler state and run log; Admin SDK only (see firestore.rules). */
export interface ContentAgentStatePort {
  /** Rotation position across (passage, tier) slots; 0 before the first recorded run. */
  getCursor(): Promise<number>;
  /** Persists the run report and the next rotation position together. */
  recordRun(report: ContentAgentRunReport, nextCursor: number): Promise<void>;
}

export function clampBatchSize(value: unknown): number {
  if (typeof value !== "number" || !Number.isInteger(value)) return DEFAULT_CONTENT_AGENT_BATCH_SIZE;
  return Math.min(Math.max(value, 1), MAX_CONTENT_AGENT_BATCH_SIZE);
}

/**
 * Passages the agent may draw from: fully reviewed (no register violations)
 * and backed by an approved generation source. Order follows the register,
 * so the rotation is deterministic.
 */
export function eligibleAgentPassages(passages: SourcePassage[], sources: ContentSource[]): SourcePassage[] {
  return passages.filter((passage) => {
    if (findPassageViolations(passage).length > 0) return false;
    const source = sources.find((candidate) => candidate.id === passage.sourceId);
    return source !== undefined && isLessonGenerationSource(source) && source.eligibleTypes.length > 0;
  });
}

function errorMessage(error: unknown): string {
  const message = error instanceof Error ? error.message : String(error);
  return message.slice(0, 500);
}

/**
 * One scheduled cycle of the continuous content agent. It picks the next
 * (verified passage, tier) slot in rotation and runs the same
 * duplicate-screened, reservation-backed `generateDraftBatch` the operator
 * CLI uses, so every candidate is checked against the full draft and
 * published inventory before it is written.
 *
 * It never publishes. Drafts still go through `approve-draft` review.
 * Disabled unless `systemConfig/global.contentGenerationEnabled === true`.
 * The cursor always advances, so one exhausted or failing passage cannot
 * stall the rotation.
 */
export async function runContentAgentCycle(deps: {
  settings: ContentAgentSettings;
  passages: SourcePassage[];
  sources: ContentSource[];
  generator: DraftGenerationPort;
  repo: ContentAdminRepositoryPort;
  state: ContentAgentStatePort;
  now: Date;
}): Promise<ContentAgentRunReport> {
  const { settings, passages, sources, generator, repo, state, now } = deps;
  const requested = clampBatchSize(settings.batchSize);
  const base: ContentAgentRunReport = {
    status: "disabled",
    startedAt: now.toISOString(),
    cursor: 0,
    passageId: null,
    sourceId: null,
    tier: null,
    requested,
    accepted: [],
    rejected: [],
    attempts: 0,
    error: null,
    published: false,
  };
  if (!settings.enabled) return base;

  const cursor = await state.getCursor();
  const eligible = eligibleAgentPassages(passages, sources);
  if (eligible.length === 0) {
    const report: ContentAgentRunReport = { ...base, status: "no-verified-passages", cursor };
    await state.recordRun(report, cursor);
    return report;
  }

  const passage = eligible[cursor % eligible.length];
  const tier = CONTENT_AGENT_TIERS[Math.floor(cursor / eligible.length) % CONTENT_AGENT_TIERS.length];
  const slot = { ...base, cursor, passageId: passage.id, sourceId: passage.sourceId, tier };
  let report: ContentAgentRunReport;
  try {
    const batch = await generateDraftBatch(
      { tier, sourceId: passage.sourceId, passageId: passage.id, count: requested },
      sources,
      generator,
      repo,
      passages,
    );
    report = {
      ...slot,
      status: batch.exhausted ? "exhausted" : "completed",
      accepted: batch.accepted.map(({ contentId, title, type }) => ({ contentId, title, type })),
      rejected: batch.rejected,
      attempts: batch.attempts,
    };
  } catch (error) {
    report = { ...slot, status: "failed", error: errorMessage(error) };
  }
  await state.recordRun(report, cursor + 1);
  return report;
}
