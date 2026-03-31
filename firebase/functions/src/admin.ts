import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

/**
 * Cloud Function: Set admin role on a user
 * Only callable by existing admins
 */
export const setAdminRole = functions.https.onCall(
  async (data: { targetUserId: string }, context) => {
    if (!context.auth) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "Must be logged in."
      );
    }

    // Verify caller is admin
    const callerDoc = await admin.firestore()
      .collection("users")
      .doc(context.auth.uid)
      .get();

    const callerData = callerDoc.data();
    if (!callerData || callerData.planType !== "ADMIN") {
      throw new functions.https.HttpsError(
        "permission-denied",
        "Admin access required."
      );
    }

    const {targetUserId} = data;
    if (!targetUserId || typeof targetUserId !== "string") {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Target user ID is required."
      );
    }

    try {
      await admin.firestore()
        .collection("users")
        .doc(targetUserId)
        .update({
          planType: "ADMIN",
          accountType: "PARENT",
        });

      return {success: true, message: `User ${targetUserId} is now admin.`};
    } catch (error) {
      console.error("Set admin role error:", error);
      throw new functions.https.HttpsError(
        "internal",
        "Failed to set admin role."
      );
    }
  }
);
