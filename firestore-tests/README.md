# Firestore security tests

Run from the repository root with Node 22+ and Java 21+:

```sh
npm --prefix firestore-tests ci
npm --prefix functions ci
npm --prefix firestore-tests test
```

The runner starts only the Firestore emulator with the `demo-wiccan` project,
loads the repository's actual `firestore.rules`, and stops the emulator after
the security suite and the production-repository transaction integration suite.
No Firebase credentials or deployed project are used. The first run downloads
the Firestore emulator JAR. `npm --prefix firestore-tests run test:rules` runs
only the security suite.

The suite verifies that existing delivery history remains readable only by its
owner, clients cannot fabricate or reset daily history, tier changes require the
server, and user revision/content/similarity reservations stay server-only. It also preserves
the existing published-content read policy: authenticated clients may read
published content; free/premium selection is enforced by the callable's verified
server tier. Direct published-content reads were already not gated by tier.
