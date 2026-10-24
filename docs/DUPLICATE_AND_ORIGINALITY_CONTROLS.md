# Duplicate and originality controls

The migration audit and the future content agent share deterministic normalization, concept fingerprints, passage-reuse checks, three-word n-gram overlap, longest common token-run analysis, and a semantic-provider interface. Fingerprints use the historical passage, practical technique, purpose, materials, procedure, and learning objective; titles and incantations cannot make an otherwise repeated concept unique.

`contentSimilarity.ts` reports exact and lexical concerns. Semantic thresholds default to 0.82 for editorial review and 0.92 for rejection, and are configurable. No production report may claim semantic coverage unless a provider was actually run; current audit output records `semanticExecuted: false`.

`originalityScreen.ts` compares authored lesson text with the inspected source passage and every indexed existing lesson. It routes suspicious n-gram or contiguous-word overlap to review. Passing automated checks never proves originality, so a hash-bound human originality review remains required before migration or publication.

Existing documents are preserved. Confirmed duplicate candidates are rejected from new generation, while duplicate existing records receive review dispositions and are never deleted or silently merged.
