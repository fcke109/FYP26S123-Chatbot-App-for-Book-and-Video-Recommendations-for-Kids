import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

/**
 * Trigger: On user created
 * Initialize user data when a new user signs up
 */
export const onUserCreated = functions.auth.user().onCreate(async (user: admin.auth.UserRecord) => {
  try {
    // Create initial user preferences if they don't exist
    const userDoc = await admin
      .firestore()
      .collection("users")
      .doc(user.uid)
      .get();

    if (!userDoc.exists) {
      await admin
        .firestore()
        .collection("users")
        .doc(user.uid)
        .set({
          id: user.uid,
          email: user.email,
          accountType: "CHILD",
          createdAt: admin.firestore.FieldValue.serverTimestamp(),
        }, {merge: true});
    }

    console.log(`User ${user.uid} created successfully`);
  } catch (error) {
    console.error("Error in onUserCreated:", error);
  }
});

/**
 * Callable function: Migrate existing users
 * Links all existing users (without accountType) to the admin parent account.
 * Run once after deploying the parent/child account system.
 * This function is idempotent — safe to run multiple times.
 */
export const migrateExistingUsers = functions.https.onCall(async (_data: any, _context: functions.https.CallableContext) => {
  try {
    const db = admin.firestore();

    // 1. Find or identify the admin account
    const adminQuery = await db
      .collection("users")
      .where("email", "==", "admin@littledino.com")
      .limit(1)
      .get();

    let adminUid: string;

    if (adminQuery.empty) {
      console.log("Admin account not found. Migration skipped.");
      return {success: false, message: "Admin account not found."};
    }

    adminUid = adminQuery.docs[0].id;
    console.log(`Found admin account: ${adminUid}`);

    // 2. Update admin to be a PARENT account type
    await db.collection("users").doc(adminUid).update({
      accountType: "PARENT",
    });

    // 3. Find all users without accountType or with CHILD type but no parentId
    const allUsersSnapshot = await db.collection("users").get();
    const childIds: string[] = [];
    let migratedCount = 0;

    const batch = db.batch();

    for (const doc of allUsersSnapshot.docs) {
      const data = doc.data();

      // Skip the admin account itself
      if (doc.id === adminUid) continue;

      // Skip users that already have a parentId set
      if (data.parentId) continue;

      // Set accountType to CHILD and link to admin parent
      batch.update(doc.ref, {
        accountType: "CHILD",
        parentId: adminUid,
      });

      childIds.push(doc.id);
      migratedCount++;
    }

    // 4. Update admin's childIds with all migrated users
    if (childIds.length > 0) {
      batch.update(db.collection("users").doc(adminUid), {
        childIds: admin.firestore.FieldValue.arrayUnion(...childIds),
      });
    }

    await batch.commit();

    console.log(`Migration complete: ${migratedCount} users linked to admin.`);
    return {
      success: true,
      migratedCount,
      adminUid,
      message: `${migratedCount} users linked to admin parent account.`,
    };
  } catch (error) {
    console.error("Migration error:", error);
    return {success: false, message: String(error)};
  }
});

/**
 * Callable function: Delete a specific Auth user
 * Admin-only function to delete Firebase Auth accounts.
 */
export const deleteAuthUser = functions.https.onCall(async (data: any, context: functions.https.CallableContext) => {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "Must be logged in.");
  }

  const callerDoc = await admin.firestore()
    .collection("users")
    .doc(context.auth.uid)
    .get();

  const callerData = callerDoc.data();
  if (!callerData || callerData.planType !== "ADMIN") {
    throw new functions.https.HttpsError("permission-denied", "Admin access required.");
  }

  const targetUid = data?.uid;
  if (!targetUid || typeof targetUid !== "string") {
    throw new functions.https.HttpsError("invalid-argument", "User UID required.");
  }

  try {
    await admin.auth().deleteUser(targetUid);
    console.log(`Auth account ${targetUid} deleted by admin ${context.auth.uid}`);
    return {success: true, message: `Auth account ${targetUid} deleted.`};
  } catch (error) {
    console.error("Delete auth user error:", error);
    return {success: false, message: String(error)};
  }
});

/**
 * Callable function: Clean up ghost accounts
 * Finds Auth users with no Firestore profile and deletes them.
 */
export const cleanGhostAccounts = functions.https.onCall(async (_data: any, context: functions.https.CallableContext) => {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "Must be logged in.");
  }

  const callerDoc = await admin.firestore()
    .collection("users")
    .doc(context.auth.uid)
    .get();

  const callerData = callerDoc.data();
  if (!callerData || callerData.planType !== "ADMIN") {
    throw new functions.https.HttpsError("permission-denied", "Admin access required.");
  }

  try {
    const listResult = await admin.auth().listUsers(1000);
    let deletedCount = 0;

    for (const authUser of listResult.users) {
      const firestoreDoc = await admin.firestore()
        .collection("users")
        .doc(authUser.uid)
        .get();

      if (!firestoreDoc.exists) {
        await admin.auth().deleteUser(authUser.uid);
        deletedCount++;
        console.log(`Ghost account deleted: ${authUser.uid} (${authUser.email})`);
      }
    }

    return {
      success: true,
      deletedCount,
      message: `${deletedCount} ghost account(s) cleaned up.`,
    };
  } catch (error) {
    console.error("Clean ghost accounts error:", error);
    return {success: false, message: String(error)};
  }
});
