import { contentSimilarityKey, isEligibleForDailyDelivery, sharesContentSimilarity, similarityReservationId } from "../contentEligibility";
import { Content, isContentType } from "../types";

describe("supported content types", () => {
  it("accepts only spell, potion and lesson", () => {
    for (const type of ["spell", "potion", "lesson"]) expect(isContentType(type)).toBe(true);
    for (const type of ["ritual", "Spell", "", " lesson", "unknown", null, undefined, 1]) expect(isContentType(type)).toBe(false);
  });

  it("never makes an unsupported type eligible for daily delivery", () => {
    const spell = { id: "s", type: "spell", tier: "premium", status: "published" } as unknown as Content;
    expect(isEligibleForDailyDelivery(spell, new Set())).toBe(true);
    for (const type of ["ritual", "RITUAL", "", undefined]) {
      expect(isEligibleForDailyDelivery({ ...spell, type } as unknown as Content, new Set())).toBe(false);
    }
  });
});

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
