package com.kidsrec.chatbot.data.model

data class GamificationProfile(
    val childUserId: String = "",
    val totalPoints: Int = 0,
    val currentLevel: Int = 1,
    val unlockedBadges: List<String> = emptyList(),
    val currentStreak: Int = 0,
    val lastActivityDate: String = ""
)