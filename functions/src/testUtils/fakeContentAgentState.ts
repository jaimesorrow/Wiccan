import { ContentAgentRunReport, ContentAgentStatePort } from "../contentAgent";

export class FakeContentAgentState implements ContentAgentStatePort {
  cursor = 0;
  runs: ContentAgentRunReport[] = [];

  async getCursor(): Promise<number> {
    return this.cursor;
  }

  async recordRun(report: ContentAgentRunReport, nextCursor: number): Promise<void> {
    this.runs.push(report);
    this.cursor = nextCursor;
  }
}
