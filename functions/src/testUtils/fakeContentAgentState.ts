import { ContentAgentRunReport, ContentAgentStatePort } from "../contentAgent";

/** In-memory twin of FirestoreContentAgentStateRepository's claim/lease semantics. */
export class FakeContentAgentState implements ContentAgentStatePort {
  cursor = 0;
  lease: { runId: string; until: number } | null = null;
  readonly claimedRunIds = new Set<string>();
  runs: ContentAgentRunReport[] = [];

  async claimSlot(runId: string, now: Date, leaseMs: number): Promise<number | null> {
    if (this.claimedRunIds.has(runId)) return null;
    if (this.lease && this.lease.until > now.getTime()) return null;
    this.claimedRunIds.add(runId);
    this.lease = { runId, until: now.getTime() + leaseMs };
    return this.cursor++;
  }

  async recordRun(runId: string, report: ContentAgentRunReport): Promise<void> {
    this.runs.push(report);
    if (this.lease?.runId === runId) this.lease = null;
  }
}
