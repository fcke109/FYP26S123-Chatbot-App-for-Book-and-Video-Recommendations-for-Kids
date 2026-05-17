package com.kidsrec.chatbot.ui.gamification

// Holds the details of a reward celebration shown to the child
data class RewardCelebration(
    val type: RewardCelebrationType = RewardCelebrationType.NONE,
    val title: String = "", // Main celebration title
    val message: String = "", // Main reward message, such as badge name or level achieved
    val subtitle: String = "" // Supporting message shown below the main reward text
)

// Defines the possible reward celebration types
enum class RewardCelebrationType {
    NONE, // No celebration should be shown
    BADGE, // Celebration for unlocking a new badge
    LEVEL_UP // Celebration for reaching a new level
}