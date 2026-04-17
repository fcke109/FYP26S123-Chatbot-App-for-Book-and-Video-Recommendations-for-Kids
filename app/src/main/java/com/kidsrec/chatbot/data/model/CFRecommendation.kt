package com.kidsrec.chatbot.data.model

data class CFRecommendation(
    val item: CFItem = CFItem(),
    val userBasedScore: Double = 0.0,
    val itemBasedScore: Double = 0.0,
    val finalScore: Double = 0.0,
    val reason: String = ""
)