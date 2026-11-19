import { ContentAgentRunReport, ContentAgentStatePort, DailyDraftAdmission, DailyDraftClaim } from "../contentAgent";

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

  async claimDailySlot(runId: string, now: Date, leaseMs: number, admission: DailyDraftAdmission): Promise<DailyDraftClaim> {
    if (this.claimedRunIds.has(runId) || (this.lease && this.lease.until > now.getTime())) return { status: "skipped" };
    const day = this.daily.get(admission.dateKey) ?? { reserved: 0, runs: 0 };
    const grant = day.runs >= admission.maxRuns ? 0 : Math.max(0, Math.min(admission.requested, admission.limit - day.reserved));
    if (grant === 0) return { status: "daily-limit-reached" };
    this.daily.set(admission.dateKey, { reserved: day.reserved + grant, runs: day.runs + 1 });
    const cursor = this.cursor++;
    this.claimedRunIds.add(runId);
    this.lease = { runId, until: now.getTime() + leaseMs };
    return { status: "claimed", cursor, reserved: grant };
  }

  async settleDailyDrafts(dateKey: string, reserved: number, written: number, countRun = true): Promise<void> {
    const day = this.daily.get(dateKey);
    if (!day) return;
    day.reserved = Math.max(0, day.reserved - Math.max(0, reserved - written));
    if (!countRun) day.runs = Math.max(0, day.runs - 1);
  }

  async recordRun(runId: string, report: ContentAgentRunReport): Promise<void> {
    this.runs.push(report);
    if (this.lease?.runId === runId) this.lease = null;
  }
}
