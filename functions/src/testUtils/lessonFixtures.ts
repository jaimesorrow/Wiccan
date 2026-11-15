import { CanonicalLesson } from "../lessons/canonicalLesson";

/** The approved reference lesson (owner-supplied example), used as the quality and duplicate baseline. */
export function thresholdGrounding(): CanonicalLesson {
  return {
    id: "threshold-grounding-calm-home",
    title: "Threshold Grounding for a Calm Home",
    slug: "threshold-grounding-for-a-calm-home",
    tradition: "Modern Wiccan cord practice",
    summary: "Create a simple three-knot cord to mark your transition from the noise of the outside world into the calm of your home.",
    introduction: "Thresholds often carry symbolic meaning because they separate one space from another. In this modern Wiccan practice, a cord becomes a physical reminder to pause, release outside tension, and enter your home with deliberate attention. The knots do not control events or other people. They hold the meaning you assign to them through reflection and repeated use.",
    lesson: [
      "Choose a quiet place near your doorway where you can work safely without blocking the entrance. Cut a piece of natural-fiber cord approximately the length of your forearm. Hold the cord loosely and take three slow breaths.",
      "For the first knot, consider what you are ready to leave outside your home. Tie the knot while breathing out slowly.",
      "For the second knot, name the quality you want to practice inside your home, such as patience, rest, honesty, or calm. Tie the knot without pulling it so tightly that the cord becomes difficult to handle.",
      "For the third knot, choose one action that will support that quality. It might be lowering your voice before a difficult conversation, putting away your phone for ten minutes, or taking one steady breath before entering.",
      "Hold the finished cord and repeat the incantation once. Place it on a small dish, hook, or shelf near the entrance. Do not attach it where it could become a tripping, choking, or fire hazard.",
      "When you return home, touch or look at the cord and remember the action represented by the third knot. The practice becomes meaningful through the behavior it helps you repeat.",
    ].join("\n\n"),
    incantation: "At this threshold, I arrive. I release what need not enter. With steady breath and chosen care, I make this home a place of peace.",
    reflection: "What tension did you choose to leave outside? What quality did you invite into your home? Which practical action can help that quality become visible in daily life?",
    practice: "For the next seven days, pause at the threshold before entering. Take one breath and repeat the action represented by the third knot. Record one sentence in your Grimoire about any change you notice in your behavior or mood.",
    safetyNote: "Keep cords away from children and animals. Do not hang the cord across a doorway, near a flame, or where it could catch on clothing. This reflective practice supports mindful behavior and does not guarantee changes in other people or outside circumstances.",
    primaryCategoryId: "cord-knot-work",
    secondaryCategoryIds: ["altars-sacred-space"],
    lessonType: "guided-practice",
    requiresPotionDisclosure: false,
    difficulty: "accessible",
    durationMinutes: 20,
    tools: [
      { name: "Natural-fiber cord", quantity: "Approximately the length of the user's forearm", required: true, substitution: "Cotton yarn or another safely handled fiber" },
      { name: "Small dish or safe storage hook", quantity: "1", required: false, substitution: "A drawer or shelf near the entrance" },
      { name: "Journal", quantity: "1", required: false, substitution: "The in-app reflection field" },
    ],
    correspondences: { elements: ["Earth"], colors: ["Brown", "Green", "White"], herbs: [], stones: [], moonPhases: [], days: [], symbols: ["Threshold", "Three knots", "Circle of home"] },
    tags: ["grounding", "calm", "home", "threshold", "cord", "mindfulness"],
    citations: [],
    contentStatus: "draft",
  };
}

/** A genuinely different practice that shares the cord tool. */
export function lunarBraidCalendar(): CanonicalLesson {
  return {
    ...thresholdGrounding(),
    id: "lunar-braid-calendar",
    title: "A Braided Cord for Tracking the Lunar Month",
    slug: "a-braided-cord-for-tracking-the-lunar-month",
    tradition: "Modern Wiccan lunar practice",
    summary: "Braid three strands together a little each night to keep an attentive, tactile record of one full lunar cycle.",
    introduction: "Many modern practitioners keep a lunar journal. This practice moves that record into the hands: a braid that grows nightly gives your attention a physical anchor and makes the shape of a month visible.",
    lesson: [
      "On the night of the new moon, sit by a window where you can see the sky. Gather three strands of yarn, each about an arm's length, and knot them together at one end.",
      "Look at the moon, or the place in the sky where it will rise, and notice one feeling you are carrying tonight.",
      "Braid a short section, about the width of two fingers, while naming that feeling under your breath.",
      "Each following night, return to the window and braid one more short section for that evening's feeling.",
      "At the next new moon, finish the braid with a loose knot and lay it on your altar. Read the braid from start to finish with your fingers and recall the month.",
    ].join("\n\n"),
    incantation: "",
    reflection: "Looking at the whole braid, which feelings returned most often, and what does the pattern suggest about your month?",
    practice: "Keep the finished braid and start a new one at the next new moon. Compare the two after a second cycle.",
    safetyNote: "Store the braid away from children and animals, who could swallow loose fibers or become tangled. Keep it away from candles and other flames.",
    primaryCategoryId: "lunar-practices",
    secondaryCategoryIds: ["cord-knot-work"],
    durationMinutes: 10,
    tools: [{ name: "Three strands of yarn", quantity: "About an arm's length each", required: true, substitution: "Embroidery floss" }],
    tags: ["reflection", "moon", "attention", "journal"],
  };
}
