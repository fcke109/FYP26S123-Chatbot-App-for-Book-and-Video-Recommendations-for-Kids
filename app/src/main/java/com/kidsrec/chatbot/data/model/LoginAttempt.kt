package com.kidsrec.chatbot.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp

//Tracks Login Attempt of users
@Keep
data class LoginAttempt(
    val id: String = "",
    val userId: String = "",
    val email: String = "",
    val success: Boolean = false,
    val failureReason: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val ipAddress: String = ""
) {
    constructor() : this("")
}
