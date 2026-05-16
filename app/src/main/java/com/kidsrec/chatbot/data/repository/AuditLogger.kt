package com.kidsrec.chatbot.data.repository

import android.util.Log
import com.google.firebase.functions.FirebaseFunctions
import javax.inject.Inject
import javax.inject.Singleton

// Handles audit logging for important user/system actions
@Singleton
class AuditLogger @Inject constructor(
    private val functions: FirebaseFunctions
) {
    // sends audit event to firebase cloud functions, does not block the app or crash if logging fails
    fun logAction(userId: String, action: String, details: String = "") {
        try {

            // Call Firebase Cloud Function to store audit log
            functions.getHttpsCallable("logAuditEvent")
                .call(mapOf("action" to action, "details" to details))
                .addOnFailureListener { e ->
                    Log.e("AuditLogger", "Failed to log audit event: $action", e)
                }
        } catch (e: Exception) {
            Log.e("AuditLogger", "Failed to send audit log", e)
        }
    }
}
