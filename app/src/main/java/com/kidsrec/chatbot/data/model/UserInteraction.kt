package com.kidsrec.chatbot.data.model

// Stores user interaction data used for recommendation calculations
data class UserInteraction(
    val userId: String = "",
    val itemId: String = "",

    // Interaction strength/score for collaborative filtering
    val weight: Double = 0.0
)