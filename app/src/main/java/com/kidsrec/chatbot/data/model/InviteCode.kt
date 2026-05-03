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
    // Snapshot of the parent's planType at the moment the code was generated.
    // The kid inherits this on redemption. Defaults to PREMIUM so legacy codes
    // (written before this field existed) still produce premium kids.
    val parentPlanType: String = "PREMIUM",
    val createdAt: Timestamp = Timestamp.now(),
    val expiresAt: Timestamp = Timestamp.now(),
    val isActive: Boolean = true,
    val maxUses: Int = 1,
    val usedCount: Int = 0,
    val childInterests: List<String> = emptyList(),
    val starterBooks: List<StarterBookSeed> = emptyList()
) {
    fun isExpired(): Boolean {
        return expiresAt.toDate().before(Date())
    }

    fun isUsable(): Boolean {
        return isActive && !isExpired() && usedCount < maxUses
    }
}