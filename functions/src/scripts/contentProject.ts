import { applicationDefault, initializeApp } from "firebase-admin/app";
import { parseArgs } from "node:util";

export const CONTENT_PROJECT_ID = "wiccan-android";

/** Reject misspelled flags and other projects before obtaining any credentials. */
export function parseContentArgs(args: string[], allowCommit: boolean): { commit: boolean; project: string | undefined } {
  const { values } = parseArgs({
    args,
    options: allowCommit
      ? { project: { type: "string" }, commit: { type: "boolean", default: false } }
      : { project: { type: "string" } },
    strict: true,
    allowPositionals: false,
  });
  const project = values.project;
  if (project !== undefined && project !== CONTENT_PROJECT_ID) {
    throw new Error(`This content workflow only targets ${CONTENT_PROJECT_ID}; received '${project}'.`);
  }
  return { commit: values.commit === true, project };
}

export function initializeContentProject(project: string | undefined): void {
  if (project !== CONTENT_PROJECT_ID) {
    throw new Error(`Explicit --project ${CONTENT_PROJECT_ID} is required for a remote operation.`);
  }
  if (process.env.FIRESTORE_EMULATOR_HOST) {
    throw new Error("Unset FIRESTORE_EMULATOR_HOST before checking or importing production content.");
  }
  initializeApp({ projectId: CONTENT_PROJECT_ID, credential: applicationDefault() });
}
