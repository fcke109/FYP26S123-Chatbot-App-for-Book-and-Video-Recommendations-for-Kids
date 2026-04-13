package com.kidsrec.chatbot.data.model

import androidx.annotation.Keep
@Keep
data class Feedback(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val category: String = "General",
    val rating: Int = 5,
    val message: String = "",
    val status: String = "NEW",
    val createdAtMillis: Long = 0L,
    val reviewedAtMillis: Long? = null,
    val adminNote: String? = null,
)
