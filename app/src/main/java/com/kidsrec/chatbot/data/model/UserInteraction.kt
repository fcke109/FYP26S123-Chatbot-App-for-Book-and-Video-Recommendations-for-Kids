package com.kidsrec.chatbot.data.model

data class UserInteraction(
    val userId: String = "",
    val itemId: String = "",
    val weight: Double = 0.0
)