package com.kidsrec.chatbot.ui.gamification

data class RewardCelebration(
    val type: RewardCelebrationType = RewardCelebrationType.NONE,
    val title: String = "",
    val message: String = "",
    val subtitle: String = ""
)

enum class RewardCelebrationType {
    NONE,
    BADGE,
    LEVEL_UP
}