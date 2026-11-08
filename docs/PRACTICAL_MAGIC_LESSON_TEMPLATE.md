# Wiccan practical lesson and Grimoire templates

Updated: November 7, 2026

## Canonical lesson template

The existing Firestore aliases remain readable during migration: `intro/introduction`, `body/lesson`, `materials/tools`, and `status/contentStatus`. New authoring uses the canonical names below. Existing document IDs and timestamps are preserved.

```json
{
  "id": "stable-document-id",
  "title": "Distinct practical title",
  "slug": "distinct-practical-title",
  "tradition": "Modern Wiccan practice",
  "summary": "What the user will learn and perform.",
  "introduction": "Purpose, context, and any necessary historical distinction.",
  "lesson": "A complete sequential guided method.",
  "incantation": "Optional original spoken passage or empty string.",
  "reflection": "One experience-specific question?",
  "practice": "One small follow-up action.",
  "safetyNote": "Activity-specific precautions and consent guidance.",
  "primaryCategoryId": "cord-knot-work",
  "secondaryCategoryIds": ["altars-sacred-space"],
  "lessonType": "guided-practice",
  "requiresPotionDisclosure": false,
  "difficulty": "accessible",
  "durationMinutes": 20,
  "tools": [{
    "name": "Natural-fiber cord",
    "quantity": "1 piece",
    "required": true,
    "substitution": "Cotton yarn or another safely handled fiber"
  }],
  "correspondences": {
    "elements": [],
    "colors": [],
    "herbs": [],
    "stones": [],
    "moonPhases": [],
    "days": [],
    "symbols": []
  },
  "tags": [],
  "citations": [],
  "contentStatus": "draft",
  "createdAt": "server timestamp",
  "updatedAt": "server timestamp"
}
```

Difficulty is descriptive metadata only and never gates access or asks the user for an experience level. During compatibility migration the current app may continue omitting it.

## Example lesson

```json
{
  "id": "threshold-grounding-calm-home",
  "title": "Threshold Grounding for a Calm Home",
  "slug": "threshold-grounding-for-a-calm-home",
  "tradition": "Modern Wiccan cord practice",
  "summary": "Create a three-knot cord as a reminder to leave outside tension at the threshold and enter your home with deliberate care.",
  "introduction": "Thresholds symbolically separate one space from another. This modern Wiccan practice uses a cord as a physical reminder to pause and choose how you enter your home. It does not control events or other people.",
  "lesson": "1. Choose a quiet place near the doorway where you can work without blocking the entrance. Cut a piece of cord approximately the length of your forearm and take three slow breaths.\n2. For the first knot, name what you are ready to leave outside and breathe out as you tie it.\n3. For the second knot, name a quality you want to practice inside your home, such as patience, rest, honesty, or calm.\n4. For the third knot, choose one action that supports that quality, such as lowering your voice before a difficult conversation or pausing before entering.\n5. Repeat the incantation once. Place the cord on a safe dish, hook, or shelf near the entrance where it cannot become a tripping, choking, or fire hazard.\n6. When you return home, look at the cord and repeat the action represented by the third knot.",
  "incantation": "At this threshold, I arrive. I release what need not enter. With steady breath and chosen care, I make this home a place of peace.",
  "reflection": "What did you choose to leave outside, and which action can make your chosen quality visible at home?",
  "practice": "For seven days, pause at the threshold, take one breath, and record one sentence in your Grimoire about the action you chose.",
  "safetyNote": "Keep cords away from children and animals. Do not hang the cord across a doorway, near a flame, or where it could catch on clothing. This reflective practice does not guarantee changes in other people or outside circumstances.",
  "primaryCategoryId": "cord-knot-work",
  "secondaryCategoryIds": ["altars-sacred-space"],
  "lessonType": "guided-practice",
  "requiresPotionDisclosure": false,
  "difficulty": "accessible",
  "durationMinutes": 20,
  "tools": [
    {
      "name": "Natural-fiber cord",
      "quantity": "Approximately the length of the user's forearm",
      "required": true,
      "substitution": "Cotton yarn or another safely handled fiber"
    },
    {
      "name": "Small dish or storage hook",
      "quantity": "1",
      "required": false,
      "substitution": "A drawer or shelf near the entrance"
    }
  ],
  "correspondences": {
    "elements": ["Earth"],
    "colors": ["Brown", "Green", "White"],
    "herbs": [],
    "stones": [],
    "moonPhases": [],
    "days": [],
    "symbols": ["Threshold", "Three knots", "Circle of home"]
  },
  "tags": ["grounding", "calm", "home", "threshold", "cord", "mindfulness"],
  "citations": [],
  "contentStatus": "draft",
  "createdAt": "server timestamp",
  "updatedAt": "server timestamp"
}
```

This example is original modern practice and makes no historical claim. If an editor adds historical, botanical, scientific, medical, or material-safety claims, each claim needs a verified citation. The source-bound production generator continues to require its reviewed passage record.

## Lesson screen

Today and Grimoire use one shared renderer in this order:

1. Small theme-aware emblem and optional Lesson of the Day label.
2. Lesson type, title, tradition, category chips, duration, and tool count.
3. Summary and introduction.
4. Tools.
5. Guided Lesson.
6. Incantation when present.
7. Reflection and follow-up Practice.
8. Safety Note.
9. Populated Correspondences.
10. Sources and Citations when present.
11. Shared potion disclosure when required.
12. Saved-to-Grimoire status and personal reflection action.

Optional fields render no empty cards and never show `null`. The layout scrolls, supports 320dp width and enlarged fonts, uses semantic Material 3 colors across all six palettes, and exposes accessible headings and descriptions.

## Grimoire

The former user-facing Library is named **Grimoire**. It is the permanent archive of lessons received through daily assignment. Assignment and saving are idempotent; the same user, lesson, and local date never create duplicate history.

Header copy:

> Every lesson you receive is kept here for you to revisit, reflect on, and practice again.

Grimoire supports title/tool/tag/category search, practical-category filters, read/reflection state, and newest/oldest/title/category sorting. Cards show title, practical category, tradition, short summary, received date, duration, read state, and reflection state. Cards use a small method icon rather than repeating the full logo.

Empty state:

> Your Grimoire is waiting. Your first daily lesson will be added here automatically after it is received.

No-results state:

> No lessons match these filters. Try another category or clear your filters.

The detail view reuses the canonical lesson renderer. User history and reflection state remain separate from canonical lesson content:

```json
{
  "userId": "authenticated-user-id",
  "lessonId": "stable-lesson-id",
  "assignedLocalDate": "YYYY-MM-DD",
  "receivedAt": "server timestamp",
  "firstOpenedAt": null,
  "lastOpenedAt": null,
  "completedAt": null,
  "reflectionText": "",
  "reflectionUpdatedAt": null
}
```

## Migration and publication

Do not rewrite production content from this template. First back up the inventory, map aliases and legacy category IDs, validate every record, preview the exact changes, preserve document IDs and curriculum order, and keep migrated records as drafts. Publication remains a separate reviewed operation.
