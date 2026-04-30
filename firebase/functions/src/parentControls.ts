import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

interface SoftDeleteChildRequest {
  childId?: string;
  pin?: string;
}

/**
 * Cloud Function: Soft-delete a child account on behalf of the parent.
 *
 * Soft-delete means: set status=BANNED on the child's user doc and stamp
 * deletedAt/deletedBy. The doc + subcollections are retained so the parent
 * can ask an admin to restore the account later.
 *
 * Authorization: caller must be the parent of the target child, and must
 * supply the PIN currently stored on the child's user doc (parentalPin).
 * Both checks run server-side so the client cannot skip them.
 */
export const softDeleteChildAccount = functions.https.onCall(
  async (data: SoftDeleteChildRequest, context) => {
    if (!context.auth) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "You must be signed in."
      );
    }

    const childId = (data?.childId ?? "").trim();
    const pin = (data?.pin ?? "").trim();

    if (!childId) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "childId is required."
      );
    }
    if (!/^\d{4}$/.test(pin)) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "PIN must be exactly 4 digits."
      );
    }

    const parentId = context.auth.uid;
    if (childId === parentId) {
      throw new functions.https.HttpsError(
        "failed-precondition",
        "You cannot remove your own account here."
      );
    }

    const db = admin.firestore();
    const parentRef = db.collection("users").doc(parentId);
    const childRef = db.collection("users").doc(childId);

    const [parentSnap, childSnap] = await Promise.all([
      parentRef.get(),
      childRef.get(),
    ]);

    if (!parentSnap.exists) {
      throw new functions.https.HttpsError(
        "not-found",
        "Parent account not found."
      );
    }
    if (!childSnap.exists) {
      throw new functions.https.HttpsError(
        "not-found",
        "Child account not found."
      );
    }

    const parentData = parentSnap.data() ?? {};
    const childData = childSnap.data() ?? {};

    // Authorization: must be a parent linked to this child either via
    // child.parentId or parent.childIds.
    const parentAccountType: string = parentData.accountType ?? "CHILD";
    const linkedViaChildField: boolean = childData.parentId === parentId;
    const childIdsField = (parentData.childIds as string[] | undefined) ?? [];
    const linkedViaParentField: boolean = childIdsField.includes(childId);

    if (
      parentAccountType !== "PARENT" ||
      (!linkedViaChildField && !linkedViaParentField)
    ) {
      throw new functions.https.HttpsError(
        "permission-denied",
        "You are not the parent of this child."
      );
    }

    // PIN check.
    const storedPin: string | undefined = childData.parentalPin;
    if (!storedPin || !/^\d{4}$/.test(storedPin)) {
      throw new functions.https.HttpsError(
        "failed-precondition",
        "No parental PIN is set for this child. Set a PIN first, then try again."
      );
    }
    if (storedPin !== pin) {
      throw new functions.https.HttpsError(
        "permission-denied",
        "Incorrect PIN."
      );
    }

    // Idempotent: bail out cleanly if already removed.
    if (childData.status === "BANNED") {
      return {
        success: true,
        alreadyRemoved: true,
        message: "This child account is already removed.",
      };
    }

    // Soft-delete: flip status and stamp metadata. Existing data is preserved.
    await childRef.update({
      status: "BANNED",
      deletedAt: admin.firestore.FieldValue.serverTimestamp(),
      deletedBy: parentId,
    });

    // Best-effort: revoke active tokens so an open session is bounced on
    // next refresh.
    try {
      await admin.auth().revokeRefreshTokens(childId);
    } catch (revokeErr) {
      console.warn("Failed to revoke refresh tokens for", childId, revokeErr);
    }

    await db.collection("auditLog").add({
      userId: parentId,
      action: "PARENT_SOFT_DELETE_CHILD",
      details: `Parent ${parentId} soft-deleted child ${childId}`,
      childId,
      timestamp: admin.firestore.FieldValue.serverTimestamp(),
    });

    return {
      success: true,
      alreadyRemoved: false,
      message: "Child account removed.",
    };
  }
);
