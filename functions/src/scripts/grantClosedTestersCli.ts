import { applicationDefault, initializeApp } from "firebase-admin/app";
import { getAuth } from "firebase-admin/auth";
import { getFirestore } from "firebase-admin/firestore";
import { parseArgs } from "node:util";
import { closedTestGrant, parseClosedTestEnd } from "../closedTestGrant";
import { CONTENT_PROJECT_ID } from "./contentProject";

/**
 * Grants closed-test premium to existing accounts by email.
 *
 * Dry-run (default) prints the plan and does not contact Firebase.
 * `--commit` requires `--project wiccan-android` and Application Default Credentials.
 * It merges tier, expiry, and the closed-test marker. It does not delete profile
 * fields and does not write a Play purchase token.
 *
 *   npm run grant-closed-testers -- --ends 2026-12-31 --emails a@x.com,b@x.com
 *   npm run grant-closed-testers -- --project wiccan-android --commit --ends 2026-12-31 --emails a@x.com
 */
function main(): Promise<void> | void {
  const { values } = parseArgs({
    args: process.argv.slice(2),
    options: {
      project: { type: "string" },
      commit: { type: "boolean", default: false },
      ends: { type: "string" },
      emails: { type: "string" },
    },
    strict: true,
  });
  if (values.project !== undefined && values.project !== CONTENT_PROJECT_ID) {
    throw new Error(`This workflow only targets ${CONTENT_PROJECT_ID}.`);
  }
  if (!values.ends || !values.emails?.trim()) {
    throw new Error("--ends and --emails are required.");
  }
  const endsAtMillis = parseClosedTestEnd(values.ends);
  const emails = [...new Set(values.emails.split(",").map((email) => email.trim().toLowerCase()).filter(Boolean))];
  if (emails.length === 0 || emails.some((email) => !email.includes("@"))) {
    throw new Error("--emails must be a comma-separated list of email addresses.");
  }
  const grant = closedTestGrant(endsAtMillis);
  console.log(JSON.stringify({
    project: CONTENT_PROJECT_ID,
    commit: values.commit === true,
    emails,
    grant,
  }, null, 2));
  if (values.commit !== true) {
    console.log("Dry run only. Re-run with --project wiccan-android --commit to write.");
    return;
  }
  if (values.project !== CONTENT_PROJECT_ID) {
    throw new Error(`Explicit --project ${CONTENT_PROJECT_ID} is required to write.`);
  }
  if (process.env.FIRESTORE_EMULATOR_HOST) {
    throw new Error("Unset FIRESTORE_EMULATOR_HOST before granting production access.");
  }
  initializeApp({ projectId: CONTENT_PROJECT_ID, credential: applicationDefault() });
  return writeGrants(emails, grant);
}

async function writeGrants(emails: string[], grant: ReturnType<typeof closedTestGrant>): Promise<void> {
  const auth = getAuth();
  const db = getFirestore();
  for (const email of emails) {
    const user = await auth.getUserByEmail(email);
    await db.collection("users").doc(user.uid).set(grant, { merge: true });
    console.log(`Granted ${email} (${user.uid}) until ${grant.subscriptionExpiresAtMillis}.`);
  }
}

try {
  Promise.resolve(main()).catch((error: unknown) => {
    console.error((error as Error).message);
    process.exitCode = 1;
  });
} catch (error) {
  console.error((error as Error).message);
  process.exitCode = 1;
}
