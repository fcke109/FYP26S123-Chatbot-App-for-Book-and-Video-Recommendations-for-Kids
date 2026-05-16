package com.kidsrec.chatbot.data.model

import androidx.annotation.Keep

// Stores notifications shown to users inside the app
@Keep
data class UserNotification(
    val id: String = "",
    val title: String = "",
    val body: String = "",

    // Notification type/category
    val type: String = "",

    // Read/unread status
    val read: Boolean = false,
    val category: String = "",

    // Creation timestamp
    val createdAt: Long = 0L
)
