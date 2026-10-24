# Historical source registry report

The shared candidate registry is `functions/historicalSourceRegistry.json`. It records all ten requested works, their historical context, the exact edition or unresolved edition requirement, access and rights evidence, and exclusions. The machine validator refuses to mark a source `approved` unless commercial-use rights are verified and at least one exact passage is approved.

## Current result

- Candidate records: **10**.
- Approved for lesson generation: **0**.
- Edition-level rights verified: **2** — the 1889 Mathers *Key of Solomon* in Wellcome Collection and the 1910 De Laurence *Sixth and Seventh Books of Moses* in the Library of Congress.
- Rights blocked pending a reusable edition or translation: **3** — *Picatrix*, the Greek Magical Papyri, and the unresolved *Book of St. Cyprian* edition.
- Other candidates remain pending passage, edition, or jurisdiction review.

Edition-level rights do not approve a ritual. The production passage register remains `functions/sourcePassages.json`; it is intentionally empty until a reviewer binds an exact locator to manuscript dating, the selected edition and translator, commercial-use rights, source fidelity, Wiccan-scope reasoning, and safety approval. As a result, migration and generator writes remain blocked by validation.

## Decisions

- “Historical grimoire” now means an identifiable ancient, medieval, or early-modern primary magical text. The application continues to identify every adapted lesson as `Wiccan / Modern Witchcraft` and stores the actual source tradition separately.
- Modern English translations are treated as copyrighted unless their reuse rights are independently verified. Free reading access is not sufficient.
- Unsafe, coercive, medical, animal-use, extreme ascetic, and hazardous combustion procedures are excluded or require a safe symbolic adaptation that preserves a defensible documented technique.
- A corpus such as the Greek Magical Papyri must identify a specific papyrus and line range; a family of books such as the *Book of St. Cyprian* must identify a specific edition.

This report is an evidence inventory, not an editorial approval record.
