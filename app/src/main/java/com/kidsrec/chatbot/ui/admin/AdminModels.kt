package com.kidsrec.chatbot.ui.admin

data class AdminStats(
    val totalUsers: Long = 0,
    val dailyActiveUsers: Long = 0,
    val monthlyActiveUsers: Long = 0,
    val chatbotUsageCount: Long = 0
)

enum class NotificationType {
    ANNOUNCEMENT,
    PERSONALIZED
}

data class AdminNotificationUiState(
    val title: String = "",
    val body: String = "",
    val type: NotificationType = NotificationType.ANNOUNCEMENT,
    val targetValue: String = ""
)
