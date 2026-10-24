import { screenOriginality } from "../originalityScreen";

describe("originality screening", () => {
  it("flags copied source wording for review", () => {
    const result = screenOriginality(
      { body: "prepare the lamp and lie down in silence to receive a dream" },
      "prepare the lamp and lie down in silence to receive a dream",
      [],
    );
    expect(result.automatedClear).toBe(false);
    expect(result.findings[0].kind).toBe("source-overlap-review");
  });

  it("compares against every existing lesson and retains mandatory human review", () => {
    const result = screenOriginality(
      { body: "1. Tie the cord. 2. Release the cord after reflection." },
      "A source describes a colored band.",
      [{ id: "existing-1", fields: { body: "1. Tie the cord. 2. Release the cord after reflection." } }],
    );
    expect(result.findings.some((finding) => finding.comparedId === "existing-1")).toBe(true);
    expect(result.humanReviewStillRequired).toBe(true);
  });
});
