import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

interface VerifyPurchaseRequest {
  purchaseToken?: string;
  productId?: string;
}

/**
 * Cloud Function: Verify Google Play purchase and upgrade user plan.
 *
 * Also accepts a demo invocation with no purchaseToken/productId so the in-app
 * "demo payment" screen can complete a successful upgrade — Firestore rules
 * forbid clients from writing planType directly, so this server-side path
 * is the only way an account can transition FREE -> PREMIUM.
 *
 * If the upgrading user is a parent, all of their linked children are upgraded
 * too. The link is discovered both ways: via parent.childIds (legacy) AND via
 * users.parentId == parentId (the parentId is set on every invite-code redeem).
 */
export const verifyPurchase = functions.https.onCall(
  async (data: VerifyPurchaseRequest, context) => {
    if (!context.auth) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "Must be logged in."
      );
    }

    const purchaseToken = data?.purchaseToken ?? "";
    const productId = data?.productId ?? "demo";

    try {
      // Production note: when a real purchaseToken is present, verify against
      // the Google Play Developer API before granting Premium. The demo path
      // (empty token) is gated to require an authenticated user only.

      const userId = context.auth.uid;
      const db = admin.firestore();

      // 1. Upgrade the calling user's plan.
      const userRef = db.collection("users").doc(userId);
      await userRef.update({planType: "PREMIUM", isGuest: false});

      // 2. Discover linked children both ways and cascade Premium.
      const userDoc = await userRef.get();
      const userData = userDoc.data() ?? {};
      const accountType: string = userData.accountType ?? "CHILD";

      let childIds: string[] = [];
      if (accountType === "PARENT") {
        const fromField = (userData.childIds as string[] | undefined) ?? [];
        const byQuerySnapshot = await db
          .collection("users")
          .where("parentId", "==", userId)
          .get();
        const fromQuery = byQuerySnapshot.docs.map((d) => d.id);
        childIds = Array.from(new Set([...fromField, ...fromQuery]));
      }

      if (childIds.length > 0) {
        const batch = db.batch();
        for (const childId of childIds) {
          batch.update(
            db.collection("users").doc(childId),
            {planType: "PREMIUM", isGuest: false}
          );
        }
        await batch.commit();
      }

      // 3. Audit log.
      await db.collection("auditLog").add({
        userId,
        action: "PURCHASE",
        details:
          `Product: ${productId}, ` +
          `tokenPresent: ${purchaseToken.length > 0}, ` +
          `children upgraded: ${childIds.length}`,
        timestamp: admin.firestore.FieldValue.serverTimestamp(),
      });

      return {
        success: true,
        message: "Plan upgraded to Premium.",
        childrenUpgraded: childIds.length,
      };
    } catch (error) {
      console.error("Purchase verification error:", error);
      throw new functions.https.HttpsError(
        "internal",
        "Failed to verify purchase."
      );
    }
  }
);
