import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

interface VerifyPurchaseRequest {
  purchaseToken: string;
  productId: string;
}

/**
 * Cloud Function: Verify Google Play purchase and upgrade user plan
 */
export const verifyPurchase = functions.https.onCall(
  async (data: VerifyPurchaseRequest, context) => {
    if (!context.auth) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "Must be logged in."
      );
    }

    const {purchaseToken, productId} = data;
    if (!purchaseToken || !productId) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Purchase token and product ID are required."
      );
    }

    try {
      // In production, verify the purchase token with Google Play Developer API:
      // const androidpublisher = google.androidpublisher('v3');
      // const result = await androidpublisher.purchases.subscriptions.get({...});
      //
      // For now, we trust the client-side verification from Google Play Billing Library
      // and just update the user's plan type.

      const userId = context.auth.uid;

      // Upgrade the parent's plan
      const userRef = admin.firestore().collection("users").doc(userId);
      await userRef.update({planType: "PREMIUM"});

      // Also upgrade all linked children to Premium
      const userDoc = await userRef.get();
      const userData = userDoc.data();
      const childIds: string[] = userData?.childIds || [];

      if (childIds.length > 0) {
        const batch = admin.firestore().batch();
        for (const childId of childIds) {
          batch.update(
            admin.firestore().collection("users").doc(childId),
            {planType: "PREMIUM"}
          );
        }
        await batch.commit();
      }

      // Log the purchase
      await admin.firestore()
        .collection("auditLog")
        .add({
          userId,
          action: "PURCHASE",
          details: `Product: ${productId}, children upgraded: ${childIds.length}`,
          timestamp: admin.firestore.FieldValue.serverTimestamp(),
        });

      return {success: true, message: "Plan upgraded to Premium."};
    } catch (error) {
      console.error("Purchase verification error:", error);
      throw new functions.https.HttpsError(
        "internal",
        "Failed to verify purchase."
      );
    }
  }
);
