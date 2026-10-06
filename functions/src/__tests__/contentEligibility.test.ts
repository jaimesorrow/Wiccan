import { contentSimilarityKey, sharesContentSimilarity, similarityReservationId } from "../contentEligibility";

describe("editorial similarity metadata", () => {
  it("groups rewordings with the same explicit concept ID", () => {
    expect(contentSimilarityKey({ similarityGroupId: "water" })).toBe(contentSimilarityKey({ similarityGroupId: "water", sourceId: "other" }));
  });

  it("uses a normalized source passage when legacy content lacks an explicit group", () => {
    expect(contentSimilarityKey({ sourceId: "ptah", sourceCitation: " Hymn  4 " }))
      .toBe(contentSimilarityKey({ sourceId: "ptah", sourceCitation: "hymn 4" }));
  });

  it("permits distinct explicit concept groups even when a broad source citation matches", () => {
    const provenance = { sourceId: "museum", sourceCitation: "Amulets article" };
    expect(contentSimilarityKey({ ...provenance, similarityGroupId: "eye" }))
      .not.toBe(contentSimilarityKey({ ...provenance, similarityGroupId: "heart" }));
  });

  it("does not confuse different passages from the same tradition or source", () => {
    expect(contentSimilarityKey({ sourceId: "ptah", sourceCitation: "Hymn 1" }))
      .not.toBe(contentSimilarityKey({ sourceId: "ptah", sourceCitation: "Hymn 2" }));
    expect(contentSimilarityKey({ sourceId: "ptah" })).toBeNull();
  });

  it("conservatively matches mixed metadata regardless of delivery order", () => {
    const legacy = { sourceId: "museum", sourceCitation: "Passage 1" };
    const editorial = { ...legacy, similarityGroupId: "concept" };
    expect(sharesContentSimilarity(legacy, editorial)).toBe(true);
    expect(sharesContentSimilarity(editorial, legacy)).toBe(true);
    expect(sharesContentSimilarity(editorial, { ...editorial, similarityGroupId: "different" })).toBe(false);
  });

  it("creates deterministic Firestore-safe reservation IDs for editorial group identifiers", () => {
    const id = similarityReservationId("editorial:group/with/slashes");
    expect(id).toMatch(/^[a-f0-9]{64}$/);
    expect(similarityReservationId("editorial:group/with/slashes")).toBe(id);
  });
});
