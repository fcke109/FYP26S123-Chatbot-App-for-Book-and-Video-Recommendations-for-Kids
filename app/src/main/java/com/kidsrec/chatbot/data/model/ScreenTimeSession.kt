package com.kidsrec.chatbot.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp

@Keep
data class ScreenTimeSession(
    val id: String = "",
    val userId: String = "",
    val date: String = "",
    val totalMinutes: Int = 0,
    val sessions: List<SessionEntry> = emptyList(),
    val extensionRequested: Boolean = false,
    val extensionGranted: Boolean = false,
    val bonusMinutes: Int = 0
) {
    constructor() : this("")
}

@Keep
data class SessionEntry(
    val startTime: Timestamp? = null,
    val endTime: Timestamp? = null,
    val durationMinutes: Int = 0
) {
    constructor() : this(null, null, 0)
}
