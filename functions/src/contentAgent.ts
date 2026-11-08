import { findPassageViolations } from "./sourcePassages";
import type { SourcePassage } from "./sourcePassages";
import { ContentSource, isLessonGenerationSource } from "./contentSource";
import { ContentTier } from "./types";
import { DraftGenerationPort } from "./draftGenerationPort";
import type { DraftGenerationRequest, GeneratedDraftFields } from "./draftGenerationPort";
import { ContentAdminRepositoryPort } from "./contentAdminRepositoryPort";
import type { NewDraftContent } from "./contentAdminRepositoryPort";
import { generateDraftBatch } from "./generateDraft";
import type { GeneratedDraftBatchResult } from "./generateDraft";

export const CONTENT_AGENT_TIERS: readonly ContentTier[] = ["free", "premium"];
export const DEFAULT_CONTENT_AGENT_BATCH_SIZE = 3;
/** generateDraftBatch makes up to 5 attempts per draft, so 5 caps a run at 25 model calls. */
export const MAX_CONTENT_AGENT_BATCH_SIZE = 5;
/** Longer than the job's 30-minute timeout, so a live run always outlasts its lease check. */
export const CONTENT_AGENT_LEASE_MS = 35 * 60_000;

export interface ContentAgentSettings {
  enabled: boolean;
  batchSize: number;
}

export type ContentAgentRunStatus =
  | "disabled"
  | "skipped"
  | "no-verified-passages"
  | "completed"
  | "exhausted"
  | "failed";

export interface ContentAgentRunReport {
  status: ContentAgentRunStatus;
  runId: string;
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
  /**
   * Atomically claims the next rotation slot for `runId` and advances the
   * cursor in the same transaction. Returns the claimed cursor, or null when
   * this runId already ran (a duplicate scheduler delivery) or another run
   * still holds an unexpired lease.
   */
  claimSlot(runId: string, now: Date, leaseMs: number): Promise<number | null>;
  /** Writes the final report under `runId` and releases that run's lease. */
  recordRun(runId: string, report: ContentAgentRunReport): Promise<void>;
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

/** Records drafts committed and model calls made, so a run that throws midway still reports them. */
class ProgressRecorder {
  attempts = 0;
  readonly accepted: ContentAgentRunReport["accepted"] = [];

  generator(inner: DraftGenerationPort): DraftGenerationPort {
    return {
      generateDraft: (request: DraftGenerationRequest): Promise<GeneratedDraftFields> => {
        this.attempts += 1;
        return inner.generateDraft(request);
      },
    };
  }

  repo(inner: ContentAdminRepositoryPort): ContentAdminRepositoryPort {
    return new Proxy(inner, {
      get: (target, property, receiver) => {
        if (property !== "createDraftWithOriginalityReservations") {
          const value = Reflect.get(target, property, receiver);
          return typeof value === "function" ? value.bind(target) : value;
        }
        return async (draft: NewDraftContent, keys: string[]) => {
          const contentId = await target.createDraftWithOriginalityReservations(draft, keys);
          this.accepted.push({ contentId, title: draft.title, type: draft.type });
          return contentId;
        };
      },
    });
  }
}

/**
 * One scheduled cycle of the continuous content agent. It claims the next
 * (verified passage, tier) slot in rotation and runs the same
 * duplicate-screened, reservation-backed `generateDraftBatch` the operator
 * CLI uses, so every candidate is checked against the full draft and
 * published inventory before it is written.
 *
 * It never publishes. Drafts still go through `approve-draft` review.
 * Disabled unless `systemConfig/global.contentGenerationEnabled === true`.
 * The slot claim is transactional and keyed by `runId`, so a duplicate or
 * overlapping scheduler delivery is skipped instead of repeating a slot.
 * The cursor advances on claim, so one exhausted or failing passage cannot
 * stall the rotation.
 */
export async function runContentAgentCycle(deps: {
  runId: string;
  settings: ContentAgentSettings;
  passages: SourcePassage[];
  sources: ContentSource[];
  generator: DraftGenerationPort;
  repo: ContentAdminRepositoryPort;
  state: ContentAgentStatePort;
  now: Date;
}): Promise<ContentAgentRunReport> {
  const { runId, settings, passages, sources, generator, repo, state, now } = deps;
  const requested = clampBatchSize(settings.batchSize);
  const base: ContentAgentRunReport = {
    status: "disabled",
    runId,
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

  const cursor = await state.claimSlot(runId, now, CONTENT_AGENT_LEASE_MS);
  if (cursor === null) return { ...base, status: "skipped" };

  const eligible = eligibleAgentPassages(passages, sources);
  if (eligible.length === 0) {
    const report: ContentAgentRunReport = { ...base, status: "no-verified-passages", cursor };
    await state.recordRun(runId, report);
    return report;
  }

  const passage = eligible[cursor % eligible.length];
  const tier = CONTENT_AGENT_TIERS[Math.floor(cursor / eligible.length) % CONTENT_AGENT_TIERS.length];
  const slot = { ...base, cursor, passageId: passage.id, sourceId: passage.sourceId, tier };
  const progress = new ProgressRecorder();
  let report: ContentAgentRunReport;
  try {
    const batch = await generateDraftBatch(
      { tier, sourceId: passage.sourceId, passageId: passage.id, count: requested },
      sources,
      progress.generator(generator),
      progress.repo(repo),
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
    // Drafts committed before the failure stay as drafts; report them so an operator can review them.
    report = {
      ...slot,
      status: "failed",
      accepted: progress.accepted,
      attempts: progress.attempts,
      error: errorMessage(error),
    };
  }
  await state.recordRun(runId, report);
  return report;
}
