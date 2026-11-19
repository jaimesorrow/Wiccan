import { ContentAgentRunReport, ContentAgentStatePort } from "../contentAgent";

/** In-memory twin of FirestoreContentAgentStateRepository's claim/lease semantics. */
export class FakeContentAgentState implements ContentAgentStatePort {
  cursor = 0;
  lease: { runId: string; until: number } | null = null;
  readonly claimedRunIds = new Set<string>();
  runs: ContentAgentRunReport[] = [];
  /** Per UTC date: slots reserved against the daily cap and runs admitted. */
  readonly daily = new Map<string, { reserved: number; runs: number }>();

  async claimSlot(runId: string, now: Date, leaseMs: number): Promise<number | null> {
    if (this.claimedRunIds.has(runId)) return null;
    if (this.lease && this.lease.until > now.getTime()) return null;
    this.claimedRunIds.add(runId);
    this.lease = { runId, until: now.getTime() + leaseMs };
    return this.cursor++;
  }

  async reserveDailyDrafts(dateKey: string, requested: number, limit: number, maxRuns: number): Promise<number> {
    const day = this.daily.get(dateKey) ?? { reserved: 0, runs: 0 };
    const grant = day.runs >= maxRuns ? 0 : Math.max(0, Math.min(requested, limit - day.reserved));
    if (grant > 0) this.daily.set(dateKey, { reserved: day.reserved + grant, runs: day.runs + 1 });
    return grant;
  }

  async settleDailyDrafts(dateKey: string, reserved: number, written: number): Promise<void> {
    const day = this.daily.get(dateKey);
    if (day) day.reserved = Math.max(0, day.reserved - Math.max(0, reserved - written));
  }

  async recordRun(runId: string, report: ContentAgentRunReport): Promise<void> {
    this.runs.push(report);
    if (this.lease?.runId === runId) this.lease = null;
  }
}
