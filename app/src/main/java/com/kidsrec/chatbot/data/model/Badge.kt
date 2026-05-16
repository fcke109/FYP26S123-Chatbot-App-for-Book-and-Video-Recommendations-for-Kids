package com.kidsrec.chatbot.data.model

// Model for achievement badges earned by users
data class Badge(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val iconName: String = "",
    val category: String = "",
    val requiredCount: Int = 0
)