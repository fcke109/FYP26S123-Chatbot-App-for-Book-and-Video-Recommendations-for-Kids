package com.kidsrec.chatbot.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp

// Stores daily screen time usage information for a user
@Keep
data class ScreenTimeSession(
    val id: String = "",
    val userId: String = "",
    val date: String = "",

    // Total usage time for the day
    val totalMinutes: Int = 0,

    // List of individual app sessions
    val sessions: List<SessionEntry> = emptyList(),

    // Parent extension request status
    val extensionRequested: Boolean = false,
    val extensionGranted: Boolean = false,

    // Extra bonus minutes granted by parent
    val bonusMinutes: Int = 0
) {
    // Empty constructor required for Firestore
    constructor() : this("")
}

// Represents a single app usage session
@Keep
data class SessionEntry(
    val startTime: Timestamp? = null,
    val endTime: Timestamp? = null,

    // Duration of the session in minutes
    val durationMinutes: Int = 0
) {

    // Empty constructor required for Firestore
    constructor() : this(null, null, 0)
}
