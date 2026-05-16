package com.kidsrec.chatbot.data.model

import com.google.firebase.Timestamp

// Stores information about badges unlocked by a user
data class BadgeUnlock(
    val badgeId: String = "",
    val badgeTitle: String = "",
    val description: String = "",
    val iconName: String = "",
    val unlockedAt: Timestamp? = null
)