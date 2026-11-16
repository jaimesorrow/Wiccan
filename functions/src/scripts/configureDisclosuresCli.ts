import { getFirestore } from "firebase-admin/firestore";
import { CONTENT_PROJECT_ID, initializeContentProject, parseContentArgs } from "./contentProject";

export const POTION_SAFETY_DISCLOSURE =
  "Wiccan's potions, teas, infusions, baths, oils, sprays, jars, and other preparations are offered " +
  "for educational and spiritual purposes, not as medical advice or treatment. Unless a lesson " +
  "explicitly says a preparation has been reviewed as food-safe for its intended use, treat it as " +
  "ritual use only: do not eat, drink, inhale, inject, or otherwise consume it. Never use a substance " +
  "you cannot positively identify. Before use, check allergies, medication interactions, pregnancy " +
  "or breastfeeding risks, skin sensitivity, child and pet toxicity, ventilation, and fire safety. " +
  "Patch-test topical preparations, stop immediately if irritation or illness occurs, and contact a " +
  "qualified healthcare professional or poison control when appropriate. Clearly label preparations " +
  "and keep them out of reach of children and animals.";

async function main(): Promise<void> {
  const args = parseContentArgs(process.argv.slice(2), true);
  initializeContentProject(args.project);
  const ref = getFirestore().collection("settings").doc("disclosures");
  const existing = await ref.get();
  const current = existing.get("potionSafetyDisclosure");

  console.log(`Firebase project: ${CONTENT_PROJECT_ID}. Mode: ${args.commit ? "write" : "dry run"}.`);
  console.log(`Document exists: ${existing.exists}. Disclosure configured: ${typeof current === "string" && current.trim().length > 0}.`);

  if (!args.commit) {
    console.log("No changes written. Rerun with --commit to merge the approved disclosure into settings/disclosures.");
    return;
  }

  await ref.set({ potionSafetyDisclosure: POTION_SAFETY_DISCLOSURE }, { merge: true });
  console.log("Configured settings/disclosures.potionSafetyDisclosure without changing other document fields.");
}

main().catch((error: unknown) => {
  console.error(error instanceof Error ? error.message : error);
  process.exitCode = 1;
});
