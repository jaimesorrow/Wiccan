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

const CONTENT_AGENT_TIERS: readonly ContentTier[] = ["free", "premium"];
/**
 * Hard ceiling on drafts written per UTC day, across every run, retry and
 * overlapping invocation. Enforced by a transactional Firestore counter
 * (ContentAgentStatePort.reserveDailyDrafts), not only by the batch size.
 */
export const DAILY_DRAFT_LIMIT = 2;
/** Bounds model spend: a day's counter admits at most this many runs, however they end. */
export const MAX_RUNS_PER_DAY = 3;
const DEFAULT_CONTENT_AGENT_BATCH_SIZE = DAILY_DRAFT_LIMIT;
/** generateDraftBatch makes up to 5 attempts per draft, so a run makes at most 10 model calls. */
const MAX_CONTENT_AGENT_BATCH_SIZE = DAILY_DRAFT_LIMIT;
/** Longer than the job's 30-minute timeout, so a live run always outlasts its lease check. */
const CONTENT_AGENT_LEASE_MS = 35 * 60_000;

export interface ContentAgentSettings {
  enabled: boolean;
  batchSize: number;
}

type ContentAgentRunStatus =
  | "disabled"
  | "skipped"
  | "daily-limit-reached"
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
   * In one transaction, reserves up to `requested` of the day's `limit`
   * draft slots on the counter for `dateKey` (a UTC YYYY-MM-DD), and counts
   * this run against `maxRuns`. Returns how many slots were reserved: 0 when
   * the limit or the run allowance is spent. A reservation counts toward the
   * limit immediately, so a retry or concurrent invocation can never push
   * the day past `limit`.
   */
  reserveDailyDrafts(dateKey: string, requested: number, limit: number, maxRuns: number): Promise<number>;
  /**
   * Gives back `reserved - written` unused slots after a run that finished
   * cleanly. Pass `countRun = false` for a run that never reached the model
   * (a skipped duplicate or overlapping delivery, or no verified passages), so
   * it does not use up the day's run allowance. Never called after a failed run, which keeps its full
   * reservation so a draft committed just before the failure can't be
   * miscounted.
   */
  settleDailyDrafts(dateKey: string, reserved: number, written: number, countRun?: boolean): Promise<void>;
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

/** The UTC calendar day a run counts against. */
export function dailyDraftDateKey(now: Date): string {
  return now.toISOString().slice(0, 10);
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
  constructor(private readonly dateKey: string, private readonly clock: () => Date) {}

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
          // The cap is per UTC day: never write a draft under slots reserved for an earlier date.
          if (dailyDraftDateKey(this.clock()) !== this.dateKey) {
            throw new Error(`The UTC date changed during generation (reserved for ${this.dateKey}); stopping to honor the daily cap.`);
          }
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
  /**
   * Current time, read before every draft is written. If the UTC date has
   * moved on from the day the slots were reserved, the run stops rather than
   * write into the next day's allowance. Defaults to the fixed `now`.
   */
  clock?: () => Date;
}): Promise<ContentAgentRunReport> {
  const { runId, settings, passages, sources, generator, repo, state, now } = deps;
  const clock = deps.clock ?? (() => now);
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

  // Reserve today's draft slots first so a spent day never burns a rotation slot or a model call.
  const dateKey = dailyDraftDateKey(now);
  const reserved = await state.reserveDailyDrafts(dateKey, requested, DAILY_DRAFT_LIMIT, MAX_RUNS_PER_DAY);
  if (reserved < 1) return { ...base, status: "daily-limit-reached", requested: 0 };
  base.requested = reserved;

  const cursor = await state.claimSlot(runId, now, CONTENT_AGENT_LEASE_MS);
  if (cursor === null) {
    // A duplicate or overlapping delivery: hand every reserved slot back.
    await state.settleDailyDrafts(dateKey, reserved, 0, false);
    return { ...base, status: "skipped" };
  }

  const eligible = eligibleAgentPassages(passages, sources);
  if (eligible.length === 0) {
    const report: ContentAgentRunReport = { ...base, status: "no-verified-passages", cursor };
    await state.settleDailyDrafts(dateKey, reserved, 0, false);
    await state.recordRun(runId, report);
    return report;
  }

  const passage = eligible[cursor % eligible.length];
  const tier = CONTENT_AGENT_TIERS[Math.floor(cursor / eligible.length) % CONTENT_AGENT_TIERS.length];
  const slot = { ...base, cursor, passageId: passage.id, sourceId: passage.sourceId, tier };
  const progress = new ProgressRecorder(dateKey, clock);
  let report: ContentAgentRunReport;
  try {
    const batch = await generateDraftBatch(
      { tier, sourceId: passage.sourceId, passageId: passage.id, count: reserved },
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
  // A failed run keeps its whole reservation; any other run returns the slots it did not use.
  if (report.status !== "failed") await state.settleDailyDrafts(dateKey, reserved, report.accepted.length);
  return report;
}
