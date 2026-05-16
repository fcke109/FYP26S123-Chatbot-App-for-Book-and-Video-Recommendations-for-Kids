package com.kidsrec.chatbot.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp

// Defines all audit actions tracked in the app
@Keep
enum class AuditAction {
    LOGIN, LOGOUT, CHAT_MESSAGE, CONTENT_OPENED,
    FILTER_CHANGED, FAVORITE_ADDED, FAVORITE_REMOVED,
    APPROVAL_REQUESTED, APPROVAL_DECIDED, PURCHASE,
    SCREEN_TIME_LIMIT_REACHED, SCREEN_TIME_EXTENSION
}

// Stores user activity logs for analytics and security tracking
@Keep
data class AuditLogEntry(
    val id: String = "",
    val userId: String = "",
    val action: String = "",
    val details: String = "",
    val timestamp: Timestamp = Timestamp.now()
) {

    // Empty constructor required for Firestore deserialization
    constructor() : this("")
}