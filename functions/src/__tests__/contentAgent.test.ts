import { clampBatchSize, eligibleAgentPassages, runContentAgentCycle } from "../contentAgent";
import type { ContentAgentSettings } from "../contentAgent";
import { ContentSource } from "../contentSource";
import type { SourcePassage } from "../sourcePassages";
import type { DraftGenerationRequest, GeneratedDraftFields } from "../draftGenerationPort";
import { FakeDraftGenerationPort } from "../testUtils/fakeDraftGenerationPort";
import { FakeContentAdminRepository } from "../testUtils/fakeContentAdminRepository";
import { FakeContentAgentState } from "../testUtils/fakeContentAgentState";

function source(id: string): ContentSource {
  return {
    id,
    title: `Ancient Source ${id}`,
    tradition: "Ancient ritual tradition",
    era: "ancient",
    sourceKind: "ancient-grimoire",
    period: "Ancient period",
    publicDomainBasis: "Ancient source; independently written summary.",
    sourceReference: `Catalogue ${id}`,
    summary: "Historically attested protective ritual material.",
    groundedFacts: ["A documented protective symbol was used in this tradition."],
    eligibleTypes: ["spell", "lesson"],
  };
}

function verifiedPassage(sourceId: string): SourcePassage {
  return {
    id: `passage-${sourceId}`,
    sourceId,
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
}

const SOURCES = [source("source-a"), source("source-b")];
const PASSAGES = [verifiedPassage("source-a"), verifiedPassage("source-b")];
const NOW = new Date("2026-11-08T04:00:00.000Z");
const ON: ContentAgentSettings = { enabled: true, batchSize: 2 };

function harness(overrides: Partial<{ settings: ContentAgentSettings; passages: SourcePassage[]; generator: FakeDraftGenerationPort }> = {}) {
  const generator = overrides.generator ?? new FakeDraftGenerationPort();
  const repo = new FakeContentAdminRepository();
  const state = new FakeContentAgentState();
  let tick = 0;
  const run = (runId = `run-${++tick}`, now = new Date(NOW.getTime() + tick * 86_400_000)) => runContentAgentCycle({
    runId,
    settings: overrides.settings ?? ON,
    passages: overrides.passages ?? PASSAGES,
    sources: SOURCES,
    generator,
    repo,
    state,
    now,
  });
  return { generator, repo, state, run };
}

describe("runContentAgentCycle", () => {
  it("does nothing while disabled: no model call, no write, no run record", async () => {
    const { generator, repo, state, run } = harness({ settings: { enabled: false, batchSize: 3 } });
    const report = await run();
    expect(report.status).toBe("disabled");
    expect(generator.calls).toHaveLength(0);
    expect(repo.docs.size).toBe(0);
    expect(state.runs).toHaveLength(0);
  });

  it("reports no-verified-passages instead of generating ungrounded content", async () => {
    const unverified = { ...verifiedPassage("source-a"), rights: { ...verifiedPassage("source-a").rights!, status: "pending" } };
    for (const passages of [[], [unverified as SourcePassage]]) {
      const { generator, repo, state, run } = harness({ passages });
      const report = await run();
      expect(report.status).toBe("no-verified-passages");
      expect(generator.calls).toHaveLength(0);
      expect(repo.docs.size).toBe(0);
      expect(state.runs).toEqual([report]);
    }
  });

  it("writes review-required drafts only and records the run", async () => {
    const { repo, state, run } = harness();
    const report = await run();
    expect(report).toMatchObject({ status: "completed", passageId: "passage-source-a", tier: "free", requested: 2, published: false });
    expect(report.accepted).toHaveLength(2);
    for (const { contentId } of report.accepted) {
      expect(repo.docs.get(contentId)?.status).toBe("draft");
    }
    expect([...repo.docs.values()].every((doc) => doc.status === "draft")).toBe(true);
    expect(state.runs).toEqual([report]);
    expect(state.cursor).toBe(1);
  });

  it("rotates through every verified passage, then the next tier", async () => {
    const { run } = harness({ settings: { enabled: true, batchSize: 1 } });
    const slots = [];
    for (let i = 0; i < 4; i++) {
      const report = await run();
      slots.push(`${report.passageId}/${report.tier}`);
    }
    expect(slots).toEqual([
      "passage-source-a/free",
      "passage-source-b/free",
      "passage-source-a/premium",
      "passage-source-b/premium",
    ]);
  });

  it("never writes a duplicate of a draft from an earlier run", async () => {
    const generator = new FakeDraftGenerationPort();
    generator.titleOverride = "Threshold Salt Line";
    const { repo, state, run } = harness({ generator, settings: { enabled: true, batchSize: 1 } });
    expect((await run()).accepted).toHaveLength(1);

    const second = await run();
    expect(second.status).toBe("exhausted");
    expect(second.accepted).toHaveLength(0);
    expect(second.rejected.length).toBeGreaterThan(0);
    expect(second.rejected.every((rejection) => rejection.status === "rejected-duplicate")).toBe(true);
    expect(repo.docs.size).toBe(1);
    expect(state.cursor).toBe(2);
  });

  it("skips a duplicate delivery of the same scheduled run", async () => {
    const { generator, state, run } = harness({ settings: { enabled: true, batchSize: 1 } });
    const first = await run("run-20261108T040000Z", NOW);
    const callsAfterFirst = generator.calls.length;
    const duplicate = await run("run-20261108T040000Z", NOW);
    expect(first.status).toBe("completed");
    expect(duplicate.status).toBe("skipped");
    expect(generator.calls).toHaveLength(callsAfterFirst);
    expect(state.runs).toEqual([first]);
    expect(state.cursor).toBe(1);
  });

  it("skips an overlapping run while another run holds the lease", async () => {
    const { generator, state, run } = harness();
    await state.claimSlot("run-in-progress", NOW, 60 * 60_000);
    const overlapping = await run("run-overlap", new Date(NOW.getTime() + 60_000));
    expect(overlapping.status).toBe("skipped");
    expect(generator.calls).toHaveLength(0);
    expect(state.cursor).toBe(1);
  });

  it("reports drafts saved before a mid-batch failure", async () => {
    class FailsOnSecondCall extends FakeDraftGenerationPort {
      override async generateDraft(request: DraftGenerationRequest): Promise<GeneratedDraftFields> {
        if (this.calls.length >= 1) {
          this.calls.push(request);
          throw new Error("rate limited");
        }
        return super.generateDraft(request);
      }
    }
    const { repo, state, run } = harness({ generator: new FailsOnSecondCall() });
    const report = await run();
    expect(report).toMatchObject({ status: "failed", error: "rate limited", attempts: 2 });
    expect(report.accepted).toHaveLength(1);
    expect(repo.docs.get(report.accepted[0].contentId)?.status).toBe("draft");
    expect(state.runs).toEqual([report]);
    expect(state.lease).toBeNull();
  });

  it("records a failed run and moves on when generation throws", async () => {
    class FailingGenerator extends FakeDraftGenerationPort {
      override async generateDraft(request: DraftGenerationRequest): Promise<GeneratedDraftFields> {
        this.calls.push(request);
        throw new Error("model unavailable");
      }
    }
    const { repo, state, run } = harness({ generator: new FailingGenerator() });
    const report = await run();
    expect(report).toMatchObject({ status: "failed", error: "model unavailable", passageId: "passage-source-a" });
    expect(repo.docs.size).toBe(0);
    expect(state.cursor).toBe(1);
  });
});

describe("content agent settings", () => {
  it("clamps the batch size to 1-5 and defaults to 3", () => {
    expect(clampBatchSize(undefined)).toBe(3);
    expect(clampBatchSize("4")).toBe(3);
    expect(clampBatchSize(2.5)).toBe(3);
    expect(clampBatchSize(0)).toBe(1);
    expect(clampBatchSize(99)).toBe(5);
    expect(clampBatchSize(4)).toBe(4);
  });

  it("only uses passages whose source is an approved generation source", () => {
    const orphan = verifiedPassage("not-in-sources");
    expect(eligibleAgentPassages([...PASSAGES, orphan], SOURCES).map((passage) => passage.id))
      .toEqual(["passage-source-a", "passage-source-b"]);
  });
});
