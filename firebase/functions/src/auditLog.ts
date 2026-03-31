import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

interface AuditLogRequest {
  action: string;
  details: string;
}

/**
 * Cloud Function: Log audit event
 * Records user actions for security monitoring
 */
export const logAuditEvent = functions.https.onCall(
  async (data: AuditLogRequest, context) => {
    if (!context.auth) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "Must be logged in."
      );
    }

    const {action, details} = data;
    if (!action || typeof action !== "string") {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Action is required."
      );
    }

    try {
      await admin.firestore()
        .collection("auditLog")
        .add({
          userId: context.auth.uid,
          action,
          details: details || "",
          timestamp: admin.firestore.FieldValue.serverTimestamp(),
          ip: context.rawRequest?.ip || "unknown",
        });

      return {success: true};
    } catch (error) {
      console.error("Audit log error:", error);
      // Don't throw — audit logging should not break the app
      return {success: false};
    }
  }
);
