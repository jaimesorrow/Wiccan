import {
  auditIllustrations,
  illustrationPublicationViolations,
  normalizedHttpsIllustrationUrl,
} from "../illustrationAudit";
import { practicalContentHash } from "../practicalLesson";

describe("illustration publication safeguards", () => {
  it("accepts HTTPS or an explicitly reviewed allow-listed bundle key", () => {
    expect(illustrationPublicationViolations({ illustrationUrl: "https://cdn.example/art.webp" })).toEqual([]);
    expect(illustrationPublicationViolations({ illustrationKey: "moon", illustrationKeyReviewed: true })).toEqual([]);
  });

  it.each([
    "http://cdn.example/art.webp",
    " https://cdn.example/art.webp",
    "https://user:secret@cdn.example/art.webp",
    "not a url",
  ])("rejects unsafe or non-canonical URLs: %s", (url) => {
    expect(normalizedHttpsIllustrationUrl(url)).toBeNull();
  });

  it("reports invalid published content, auditable drafts, and reused artwork", () => {
    const report = auditIllustrations([
      { id: "published-missing", fields: { status: "published", title: "Missing" } },
      { id: "draft-missing", fields: { status: "draft", title: "Draft" } },
      { id: "one", fields: { status: "draft", illustrationUrl: "https://cdn.example/shared.webp" } },
      { id: "two", fields: { status: "published", illustrationUrl: "https://cdn.example/shared.webp" } },
    ]);
    expect(report.totals).toEqual({
      documents: 4,
      published: 2,
      missingOrInvalid: 2,
      invalidPublished: 1,
      duplicateReferences: 1,
    });
    expect(report.missingOrInvalid.map((record) => record.id)).toEqual(["published-missing", "draft-missing"]);
    expect(report.duplicateReferences).toEqual([{
      reference: "url:https://cdn.example/shared.webp",
      contentIds: ["one", "two"],
    }]);
  });

  it("binds reviewed illustration fields into the publication hash", () => {
    const base = { title: "A lesson", illustrationKey: "moon", illustrationKeyReviewed: true };
    expect(practicalContentHash(base)).not.toBe(practicalContentHash({ ...base, illustrationKey: "candle" }));
    expect(practicalContentHash(base)).not.toBe(practicalContentHash({ ...base, illustrationKeyReviewed: false }));
  });
});
