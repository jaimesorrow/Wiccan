import { DAILY_DRAFT_LIMIT, MAX_RUNS_PER_DAY, dailyDraftDateKey, runContentAgentCycle } from "../contentAgent";
import type { ContentAgentSettings } from "../contentAgent";
import { ContentSource } from "../contentSource";
import type { SourcePassage } from "../sourcePassages";
import type { DraftGenerationRequest, GeneratedDraftFields } from "../draftGenerationPort";
import { FakeDraftGenerationPort } from "../testUtils/fakeDraftGenerationPort";
import { FakeContentAdminRepository } from "../testUtils/fakeContentAdminRepository";
import { FakeContentAgentState } from "../testUtils/fakeContentAgentState";

const SOURCE: ContentSource = {
  id: "source-a",
  title: "Ancient Source",
  tradition: "Ancient ritual tradition",
  era: "ancient",
  sourceKind: "ancient-grimoire",
  period: "Ancient period",
  publicDomainBasis: "Ancient source; independently written summary.",
  sourceReference: "Catalogue A",
  summary: "Historically attested protective ritual material.",
  groundedFacts: ["A documented protective symbol was used in this tradition."],
  eligibleTypes: ["spell", "potion", "lesson"],
};

const PASSAGE: SourcePassage = {
  id: "passage-source-a",
  sourceId: "source-a",
  textTitle: "Ancient Source",
  manuscriptId: "Museum 1",
  tradition: "Ancient ritual tradition",
  manuscriptDate: "Third century CE",
  edition: "Public-domain edition",
  editionDate: "1904",
  translator: "Test Translator",
  locator: "Tablet II, lines 4–8",
  url: "https://example.org/ancient-text",
  inspectedPassage: "An editor-inspected test passage.",
  attestedInstructions: ["Prepare the material.", "Complete the operation."],
  uncertainties: [],
  formula: null,
  formulaOmissionReason: "No formula is present.",
  dating: { era: "ancient", status: "verified", evidenceUrl: "https://example.org/catalogue", notes: "Catalogue date." },
  rights: { status: "verified", basis: "Public domain test edition.", reference: "https://example.org/rights", jurisdictions: ["US"] },
  scope: { status: "approved", rationale: "Documented method used as a modern witchcraft adaptation.", evidenceUrl: "https://example.org/scope" },
  review: { passageInspected: true, sourceFidelityApproved: true, safetyApproved: true, reviewedBy: "editor", reviewedAt: "2026-10-19T00:00:00.000Z" },
};

const PASSAGES: SourcePassage[] = [
  PASSAGE,
  { ...PASSAGE, id: "passage-source-b", locator: "Tablet III, lines 1–6", inspectedPassage: "A second editor-inspected test passage." },
  { ...PASSAGE, id: "passage-source-c", locator: "Tablet IV, lines 2–9", inspectedPassage: "A third editor-inspected test passage." },
  { ...PASSAGE, id: "passage-source-d", locator: "Tablet V, lines 1–4", inspectedPassage: "A fourth editor-inspected test passage." },
  { ...PASSAGE, id: "passage-source-e", locator: "Tablet VI, lines 3–8", inspectedPassage: "A fifth editor-inspected test passage." },
  { ...PASSAGE, id: "passage-source-f", locator: "Tablet VII, lines 2–5", inspectedPassage: "A sixth editor-inspected test passage." },
];

const DAY = new Date("2026-11-08T04:00:00.000Z");
const at = (hours: number, day = DAY) => new Date(day.getTime() + hours * 3_600_000);
const NEXT_DAY = new Date("2026-11-09T04:00:00.000Z");

class FailingGenerator extends FakeDraftGenerationPort {
  override async generateDraft(request: DraftGenerationRequest): Promise<GeneratedDraftFields> {
    this.calls.push(request);
    throw new Error("model unavailable");
  }
}

function harness(settings: ContentAgentSettings = { enabled: true, batchSize: 5 }, generator = new FakeDraftGenerationPort()) {
  const repo = new FakeContentAdminRepository();
  const state = new FakeContentAgentState();
  const run = (runId: string, now: Date, clock?: () => Date, passages: SourcePassage[] = PASSAGES) => runContentAgentCycle({
    runId, settings, passages, sources: [SOURCE], generator, repo, state, now, clock,
  });
  return { generator, repo, state, run };
}

describe("daily draft cap", () => {
  it("is exactly two drafts a day", () => {
    expect(DAILY_DRAFT_LIMIT).toBe(2);
  });

  it("keys the counter by the UTC date, not the local one", () => {
    expect(dailyDraftDateKey(new Date("2026-11-08T23:59:59.999Z"))).toBe("2026-11-08");
    expect(dailyDraftDateKey(new Date("2026-11-09T00:00:00.000Z"))).toBe("2026-11-09");
  });

  it("writes two drafts even when the batch size setting asks for five", async () => {
    const { repo, state, run } = harness({ enabled: true, batchSize: 5 });
    const first = await run("run-1", DAY);
    // A run may reserve two slots, but one sourcePassageId can yield only one item.
    expect(first.status).toBe("exhausted");
    expect(first.requested).toBe(2);
    expect(first.accepted).toHaveLength(1);
    const second = await run("run-2", at(1));
    expect(second.accepted).toHaveLength(1);
    expect(repo.docs.size).toBe(2);
    expect(state.daily.get("2026-11-08")).toEqual({ reserved: 2, runs: 2 });
  });

  it("writes nothing more once two one-item runs have filled the UTC day", async () => {
    const { generator, repo, state, run } = harness();
    expect((await run("run-1", DAY)).accepted).toHaveLength(1);
    expect((await run("run-2", at(1))).accepted).toHaveLength(1);
    const callsAfterCap = generator.calls.length;
    const cursorAfterCap = state.cursor;

    for (const [id, hours] of [["run-retry", 5], ["run-late", 19]] as const) {
      const report = await run(id, at(hours));
      expect(report.status).toBe("daily-limit-reached");
      expect(report.accepted).toHaveLength(0);
    }
    expect(repo.docs.size).toBe(2);
    expect(generator.calls).toHaveLength(callsAfterCap);
    // A spent day must not burn a rotation slot or leave a run record.
    expect(state.cursor).toBe(cursorAfterCap);
    expect(state.runs).toHaveLength(2);
  });

  it("writes two more once the UTC date rolls over", async () => {
    const { repo, run } = harness();
    expect((await run("run-1", DAY)).accepted).toHaveLength(1);
    expect((await run("run-2", at(1))).accepted).toHaveLength(1);
    expect((await run("run-3", at(19))).status).toBe("daily-limit-reached");
    expect((await run("run-4", NEXT_DAY)).accepted).toHaveLength(1);
    expect((await run("run-5", at(1, NEXT_DAY))).accepted).toHaveLength(1);
    expect(repo.docs.size).toBe(4);
  });

  it("lets two one-draft runs fill the day but never a third", async () => {
    const { repo, run } = harness({ enabled: true, batchSize: 1 });
    expect((await run("run-1", DAY)).accepted).toHaveLength(1);
    expect((await run("run-2", at(1))).accepted).toHaveLength(1);
    const third = await run("run-3", at(2));
    expect(third.status).toBe("daily-limit-reached");
    expect(repo.docs.size).toBe(2);
  });

  it("returns unused slots after a clean run that wrote fewer drafts, within the run allowance", async () => {
    const generator = new FakeDraftGenerationPort();
    generator.titleOverride = "Threshold Salt Line";
    const { repo, state, run } = harness({ enabled: true, batchSize: 2 }, generator);
    // Run 1 writes one draft; the rest are duplicates of it, so the run is exhausted at 1.
    const first = await run("run-1", DAY);
    expect(first.status).toBe("exhausted");
    expect(first.accepted).toHaveLength(1);
    expect(state.daily.get("2026-11-08")).toEqual({ reserved: 1, runs: 1 });
    expect(repo.docs.size).toBe(1);
  });

  it("bounds model spend: the run allowance caps even zero-draft days", async () => {
    const generator = new FakeDraftGenerationPort();
    generator.titleOverride = "Threshold Salt Line";
    const { repo, run } = harness({ enabled: true, batchSize: 1 }, generator);
    await run("run-0", DAY); // writes the one title; every later run only produces duplicates
    const statuses: string[] = [];
    for (let i = 1; i <= MAX_RUNS_PER_DAY + 1; i++) statuses.push((await run(`run-${i}`, at(i))).status);
    expect(statuses.filter((status) => status === "daily-limit-reached").length).toBeGreaterThanOrEqual(1);
    expect(repo.docs.size).toBe(1);
  });

  it("counts a failed run against the day: its reservation is kept", async () => {
    const { repo, state, run } = harness({ enabled: true, batchSize: 2 }, new FailingGenerator());
    const failed = await run("run-1", DAY);
    expect(failed.status).toBe("failed");
    expect(state.daily.get("2026-11-08")).toEqual({ reserved: 2, runs: 1 });
    const retry = await run("run-2", at(1));
    expect(retry.status).toBe("daily-limit-reached");
    expect(repo.docs.size).toBe(0);
  });

  it("hands the slots back when a duplicate scheduler delivery is skipped", async () => {
    const { repo, state, run } = harness();
    await state.claimSlot("run-same", DAY, 1); // run-same already ran
    const duplicate = await run("run-same", at(1));
    expect(duplicate.status).toBe("skipped");
    expect(state.daily.has("2026-11-08")).toBe(false);
    const real = await run("run-real", at(2));
    expect(real.accepted).toHaveLength(1);
    expect((await run("run-real-2", at(3))).accepted).toHaveLength(1);
    expect(repo.docs.size).toBe(2);
  });

  it("does not spend the run allowance on skipped duplicate or overlapping deliveries", async () => {
    const { repo, state, run } = harness({ enabled: true, batchSize: 1 });
    expect((await run("run-1", DAY)).accepted).toHaveLength(1);
    // Two redeliveries of run-1 make no model call and must not count as runs.
    expect((await run("run-1", at(1))).status).toBe("skipped");
    expect((await run("run-1", at(2))).status).toBe("skipped");
    expect(state.daily.get("2026-11-08")).toEqual({ reserved: 1, runs: 1 });
    const next = await run("run-2", at(3));
    expect(next.accepted).toHaveLength(1);
    expect(repo.docs.size).toBe(2);
    expect((await run("run-3", at(4))).status).toBe("daily-limit-reached");
  });

  it("does not spend the run allowance when there are no verified passages", async () => {
    const { state, run } = harness({ enabled: true, batchSize: 1 });
    for (let i = 0; i < MAX_RUNS_PER_DAY + 2; i++) {
      expect((await run(`run-empty-${i}`, at(i), undefined, [])).status).toBe("no-verified-passages");
    }
    expect(state.daily.get("2026-11-08")).toEqual({ reserved: 0, runs: 0 });
    expect((await run("run-real", at(8))).accepted).toHaveLength(1);
  });

  it("returns an unused slot the same UTC day, so a late reserved-2 run cannot fill the next day", async () => {
    const { repo, state, run } = harness({ enabled: true, batchSize: 2 });
    const justBeforeMidnight = new Date("2026-11-08T23:59:59.000Z");
    const report = await run("run-late", justBeforeMidnight);
    // The passage cap rejects a second item without writing, so unused slots
    // return to the reserved day rather than carrying into the next one.
    expect(report.status).toBe("exhausted");
    expect(report.accepted).toHaveLength(1);
    expect(repo.docs.size).toBe(1);
    expect(state.daily.get("2026-11-08")).toEqual({ reserved: 1, runs: 1 });
    const nextDay = await run("run-next", NEXT_DAY);
    expect(nextDay.accepted).toHaveLength(1);
    expect(state.daily.get("2026-11-09")).toEqual({ reserved: 1, runs: 1 });
    expect(repo.docs.size).toBe(2);
  });

  it("writes nothing if the date has already rolled over before the first draft", async () => {
    const { repo, run } = harness({ enabled: true, batchSize: 2 });
    const report = await run("run-late", new Date("2026-11-08T23:59:59.000Z"), () => new Date("2026-11-09T00:00:00.000Z"));
    expect(report.status).toBe("failed");
    expect(repo.docs.size).toBe(0);
  });

  it("cannot exceed two drafts when invocations overlap", async () => {
    const { repo, run } = harness();
    const reports = await Promise.all([run("run-a", DAY), run("run-b", DAY), run("run-c", DAY)]);
    const written = reports.reduce((sum, report) => sum + report.accepted.length, 0);
    expect(written).toBeLessThanOrEqual(DAILY_DRAFT_LIMIT);
    expect(repo.docs.size).toBe(written);
  });

  it("stays off, and spends nothing, while the switch is off", async () => {
    const { generator, state, run } = harness({ enabled: false, batchSize: 2 });
    const report = await run("run-1", DAY);
    expect(report.status).toBe("disabled");
    expect(generator.calls).toHaveLength(0);
    expect(state.daily.size).toBe(0);
  });

  it("only ever writes drafts of an allowed type", async () => {
    const { repo, run } = harness();
    await run("run-1", DAY);
    await run("run-2", at(1));
    await run("run-3", NEXT_DAY);
    await run("run-4", at(1, NEXT_DAY));
    expect(repo.docs.size).toBe(4);
    for (const doc of repo.docs.values()) {
      expect(["spell", "potion", "lesson"]).toContain(doc.type);
      expect(doc.status).toBe("draft");
    }
  });
});
