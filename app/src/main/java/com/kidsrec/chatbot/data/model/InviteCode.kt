package com.kidsrec.chatbot.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp

@Keep
data class InviteCode(
    val code: String = "",
    val parentId: String = "",
    val parentName: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val expiresAt: Timestamp = Timestamp.now(),
    val used: Boolean = false,
    val childInterests: List<String> = emptyList(),
    val starterBooks: List<StarterBookSeed> = emptyList()
)
