package com.kidsrec.chatbot.data.model

import androidx.annotation.Keep

@Keep
data class UserNotification(
    val id: String = "",
    val title: String = "",
    val body: String = "",
    val type: String = "",
    val read: Boolean = false,
    val category: String = "",
    val createdAt: Long = 0L,
    val popupOnLogin: Boolean = true,
    val popupShown: Boolean = false
)