// Deletes every user + favorites + readingHistory doc whose id begins with
// "synthetic_". Safe to run any time — only touches data created by
// seedSyntheticUsers.ts.
//
// Run with:
//   cd firebase/functions
//   npm run cleanup:synthetic -- --credentials="C:\path\to\serviceAccount.json"
//
// Or rely on GOOGLE_APPLICATION_CREDENTIALS env var.

import * as admin from "firebase-admin";
import * as fs from "fs";
import * as path from "path";

function readFirebaseProjectId(): string | undefined {
  const candidates = [
    path.resolve(process.cwd(), "..", ".firebaserc"),
    path.resolve(process.cwd(), ".firebaserc"),
  ];
  for (const p of candidates) {
    if (fs.existsSync(p)) {
      try {
        const rc = JSON.parse(fs.readFileSync(p, "utf8")) as {
          projects?: { default?: string };
        };
        if (rc.projects?.default) return rc.projects.default;
      } catch {
        // fall through
      }
    }
  }
  return undefined;
}

function initAdmin(): void {
  let credentialsPath: string | undefined;
  for (const arg of process.argv.slice(2)) {
    const [key, value] = arg.split("=");
    if (key === "--credentials" && value) credentialsPath = value;
  }
  const projectId = readFirebaseProjectId();
  if (credentialsPath) {
    const absPath = path.resolve(credentialsPath);
    if (!fs.existsSync(absPath)) {
      throw new Error(`Service account file not found: ${absPath}`);
    }
    const serviceAccount = JSON.parse(fs.readFileSync(absPath, "utf8"));
    admin.initializeApp({
      credential: admin.credential.cert(serviceAccount),
      projectId: serviceAccount.project_id || projectId,
    });
    return;
  }
  if (!process.env.GOOGLE_APPLICATION_CREDENTIALS) {
    throw new Error(
      "No credentials. Either pass --credentials=path\\to\\serviceAccount.json " +
        "or set GOOGLE_APPLICATION_CREDENTIALS."
    );
  }
  admin.initializeApp({
    credential: admin.credential.applicationDefault(),
    projectId,
  });
}

async function deleteSubcollection(
  db: admin.firestore.Firestore,
  parentPath: string,
  subName: string
): Promise<number> {
  const snap = await db.collection(`${parentPath}/${subName}`).get();
  if (snap.empty) return 0;
  const batch = db.batch();
  for (const doc of snap.docs) batch.delete(doc.ref);
  await batch.commit();
  return snap.size;
}

async function main(): Promise<void> {
  initAdmin();
  const db = admin.firestore();

  const usersSnap = await db
    .collection("users")
    .orderBy(admin.firestore.FieldPath.documentId())
    .startAt("synthetic_")
    .endAt("synthetic_\uf8ff")
    .get();

  console.log(`Found ${usersSnap.size} synthetic users. Deleting…`);

  let deletedFavs = 0;
  let deletedHistory = 0;
  for (const userDoc of usersSnap.docs) {
    const uid = userDoc.id;
    deletedFavs += await deleteSubcollection(db, `favorites/${uid}`, "items");
    deletedHistory += await deleteSubcollection(
      db,
      `readingHistory/${uid}`,
      "items"
    );
    await db.collection("favorites").doc(uid).delete().catch(() => undefined);
    await db.collection("readingHistory").doc(uid).delete().catch(() => undefined);
    await userDoc.ref.delete();
  }

  console.log(
    `Deleted ${usersSnap.size} users, ${deletedFavs} favorites, ${deletedHistory} reading-history entries.`
  );
}

if (require.main === module) {
  main().catch((err) => {
    console.error(err);
    process.exit(1);
  });
}
