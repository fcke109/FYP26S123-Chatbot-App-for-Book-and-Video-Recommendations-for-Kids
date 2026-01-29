import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

/**
 * Trigger: On user created
 * Initialize user data when a new user signs up
 */
export const onUserCreated = functions.auth.user().onCreate(async (user) => {
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
          createdAt: admin.firestore.FieldValue.serverTimestamp(),
        }, {merge: true});
    }

    console.log(`User ${user.uid} created successfully`);
  } catch (error) {
    console.error("Error in onUserCreated:", error);
  }
});
