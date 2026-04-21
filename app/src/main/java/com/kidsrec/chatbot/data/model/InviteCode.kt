package com.kidsrec.chatbot.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import java.util.Date

@Keep
data class InviteCode(
    val code: String = "",
    val createdBy: String = "",
    val assignedParentId: String = "",
    val parentName: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val expiresAt: Timestamp = Timestamp.now(),
    val isActive: Boolean = true,
    val maxUses: Int = 1,
    val usedCount: Int = 0
) {
    fun isExpired(): Boolean {
        return expiresAt.toDate().before(Date())
    }

    fun isUsable(): Boolean {
        return isActive && !isExpired() && usedCount < maxUses
    }
}