package com.kidsrec.chatbot.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import java.util.Date

// Stores parent-generated invite codes used for linking child accounts
@Keep
data class InviteCode(
    val code: String = "",
    val createdBy: String = "",
    val assignedParentId: String = "",
    val parentName: String = "",

    // Parent's subscription plan copied to child during registration
    // Defaults to PREMIUM for compatibility with older invite codes
    val parentPlanType: String = "PREMIUM",

    val createdAt: Timestamp = Timestamp.now(),
    val expiresAt: Timestamp = Timestamp.now(),

    // Invite code settings
    val isActive: Boolean = true,
    val maxUses: Int = 1,
    val usedCount: Int = 0,

    // Child onboarding preferences
    val childInterests: List<String> = emptyList(),
    val starterBooks: List<StarterBookSeed> = emptyList()
) {
    // Checks whether the invite code has expired
    fun isExpired(): Boolean {
        return expiresAt.toDate().before(Date())
    }

    // Checks if the code can still be used
    fun isUsable(): Boolean {
        return isActive && !isExpired() && usedCount < maxUses
    }
}