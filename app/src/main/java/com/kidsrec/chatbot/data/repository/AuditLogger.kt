package com.kidsrec.chatbot.data.repository

import android.util.Log
import com.google.firebase.functions.FirebaseFunctions
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuditLogger @Inject constructor(
    private val functions: FirebaseFunctions
) {
    /**
     * Fire-and-forget audit log. Does not block or throw on failure.
     */
    fun logAction(userId: String, action: String, details: String = "") {
        try {
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
