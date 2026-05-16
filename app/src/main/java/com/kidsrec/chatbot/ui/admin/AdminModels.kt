package com.kidsrec.chatbot.ui.admin

// Holds summary statistics displayed on the admin dashboard
data class AdminStats(
    val totalUsers: Long = 0,
    val dailyActiveUsers: Long = 0,
    val monthlyActiveUsers: Long = 0,
    val chatbotUsageCount: Long = 0
)

// Defines the types of notifications the admin can send
enum class NotificationType {
    ANNOUNCEMENT,
    PERSONALIZED
}

// Stores the current input state for the admin notification form
data class AdminNotificationUiState(
    val title: String = "",
    val body: String = "",
    val type: NotificationType = NotificationType.ANNOUNCEMENT,
    val targetValue: String = ""
)
