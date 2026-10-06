import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { after, before, beforeEach, describe, it } from "node:test";
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from "@firebase/rules-unit-testing";
import {
  collection,
  deleteDoc,
  doc,
  getDoc,
  getDocs,
  orderBy,
  query,
  setDoc,
  updateDoc,
  where,
} from "firebase/firestore";

const OWNER = "owner";
const OTHER = "other";
const HISTORY_PATH = `userContentHistory/${OWNER}_2026-10-06`;
const LEGACY_HISTORY = {
  userId: OWNER,
  contentId: "historical-lesson",
  deliveredDate: "2026-10-06",
  title: "A historically received lesson",
  type: "lesson",
  tradition: "Ancient Greek traditions",
  category: "MoonAndSeasonal",
};
let environment;

before(async () => {
  // initializeTestEnvironment requires the emulator and never connects to a
  // deployed database. The explicit demo project further guards the runner.
  environment = await initializeTestEnvironment({
    projectId: "demo-wiccan",
    firestore: {
      rules: await readFile(new URL("../firestore.rules", import.meta.url), "utf8"),
    },
  });
});

after(async () => {
  await environment?.cleanup();
});

beforeEach(async () => {
  await environment.clearFirestore();
  await environment.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await Promise.all([
      setDoc(doc(db, HISTORY_PATH), LEGACY_HISTORY),
      setDoc(doc(db, `userContentHistory/${OTHER}_2026-10-06`), {
        ...LEGACY_HISTORY,
        userId: OTHER,
        contentId: "another-users-lesson",
      }),
      setDoc(doc(db, `users/${OWNER}`), { tier: "free" }),
      setDoc(doc(db, `users/${OTHER}`), { tier: "premium" }),
      setDoc(doc(db, "content/published-free"), { status: "published", tier: "free" }),
      setDoc(doc(db, "content/published-premium"), { status: "published", tier: "premium" }),
      setDoc(doc(db, "content/draft"), { status: "draft", tier: "free" }),
      setDoc(doc(db, `userContentUsage/${OWNER}`), { revision: 1 }),
      setDoc(doc(db, `userContentUsage/${OWNER}/content/historical-lesson`), {
        deliveredDate: "2026-10-06",
      }),
      setDoc(doc(db, `userContentUsage/${OWNER}/similarityGroups/group-hash`), {
        deliveredDate: "2026-10-06",
      }),
    ]);
  });
});

function ownerDb() {
  return environment.authenticatedContext(OWNER).firestore();
}

describe("delivered-content history used by Grimoire", () => {
  it("keeps legacy history readable without new similarity metadata or renamed keys", async () => {
    const snapshot = await assertSucceeds(getDoc(doc(ownerDb(), HISTORY_PATH)));
    assert.deepEqual(snapshot.data(), LEGACY_HISTORY);
  });

  it("allows the owner's timeline query and preserves its delivered-date ordering", async () => {
    await environment.withSecurityRulesDisabled(async (context) => {
      await setDoc(doc(context.firestore(), `userContentHistory/${OWNER}_2026-10-05`), {
        ...LEGACY_HISTORY,
        deliveredDate: "2026-10-05",
        contentId: "older-lesson",
      });
    });
    const snapshot = await assertSucceeds(getDocs(query(
      collection(ownerDb(), "userContentHistory"),
      where("userId", "==", OWNER),
      orderBy("deliveredDate", "desc"),
    )));
    assert.deepEqual(snapshot.docs.map((row) => row.data().contentId), [
      "historical-lesson",
      "older-lesson",
    ]);
  });

  it("rejects another user's and unauthenticated history reads", async () => {
    await assertFails(getDoc(doc(environment.authenticatedContext(OTHER).firestore(), HISTORY_PATH)));
    await assertFails(getDoc(doc(environment.unauthenticatedContext().firestore(), HISTORY_PATH)));
  });

  it("rejects collection queries that could expose another user's history", async () => {
    await assertFails(getDocs(collection(ownerDb(), "userContentHistory")));
    await assertFails(getDocs(query(
      collection(ownerDb(), "userContentHistory"),
      where("userId", "==", OTHER),
    )));
  });

  it("rejects fabricated daily assignments even when the owner and deterministic ID match", async () => {
    await assertFails(setDoc(doc(ownerDb(), `userContentHistory/${OWNER}_2026-10-07`), {
      ...LEGACY_HISTORY,
      deliveredDate: "2026-10-07",
      contentId: "client-picked-lesson",
    }));
  });

  it("rejects modifying or deleting delivery history to repeat a lesson", async () => {
    await assertFails(updateDoc(doc(ownerDb(), HISTORY_PATH), { contentId: "replacement" }));
    await assertFails(deleteDoc(doc(ownerDb(), HISTORY_PATH)));
  });
});

describe("verified server tier", () => {
  it("allows reading one's own profile and rejects another user's profile", async () => {
    const ownProfile = await assertSucceeds(getDoc(doc(ownerDb(), `users/${OWNER}`)));
    assert.equal(ownProfile.data().tier, "free");
    await assertFails(getDoc(doc(ownerDb(), `users/${OTHER}`)));
  });

  it("rejects tier escalation, profile replacement and client profile creation", async () => {
    await assertFails(updateDoc(doc(ownerDb(), `users/${OWNER}`), { tier: "premium" }));
    await assertFails(setDoc(doc(ownerDb(), `users/${OWNER}`), { tier: "premium" }));
    await assertFails(setDoc(doc(environment.authenticatedContext("new-user").firestore(), "users/new-user"), {
      tier: "premium",
    }));
  });
});

describe("server-authoritative usage reservations", () => {
  for (const path of [
    `userContentUsage/${OWNER}`,
    `userContentUsage/${OWNER}/content/historical-lesson`,
    `userContentUsage/${OWNER}/similarityGroups/group-hash`,
  ]) {
    it(`rejects reads, writes and resets of ${path}`, async () => {
      const marker = doc(ownerDb(), path);
      await assertFails(getDoc(marker));
      await assertFails(setDoc(marker, { deliveredDate: "2026-10-07" }));
      await assertFails(deleteDoc(marker));
    });
  }

  it("rejects clients pre-reserving new content or similarity groups", async () => {
    await assertFails(setDoc(doc(ownerDb(), `userContentUsage/${OWNER}/content/new-lesson`), {
      deliveredDate: "2026-10-07",
    }));
    await assertFails(setDoc(doc(ownerDb(), `userContentUsage/${OWNER}/similarityGroups/new-group`), {
      deliveredDate: "2026-10-07",
    }));
  });
});

describe("existing published-content read policy", () => {
  it("allows authenticated published reads without changing free/premium assignment eligibility", async () => {
    await assertSucceeds(getDoc(doc(ownerDb(), "content/published-free")));
    await assertSucceeds(getDoc(doc(ownerDb(), "content/published-premium")));
  });

  it("rejects anonymous reads, draft reads and client content writes", async () => {
    await assertFails(getDoc(doc(environment.unauthenticatedContext().firestore(), "content/published-free")));
    await assertFails(getDoc(doc(ownerDb(), "content/draft")));
    await assertFails(updateDoc(doc(ownerDb(), "content/published-free"), { status: "draft" }));
    await assertFails(setDoc(doc(ownerDb(), "content/client-created"), { status: "published", tier: "free" }));
  });
});
