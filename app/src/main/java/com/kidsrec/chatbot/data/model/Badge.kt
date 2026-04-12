package com.kidsrec.chatbot.data.model

data class Badge(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val iconName: String = "",
    val category: String = "",
    val requiredCount: Int = 0
)