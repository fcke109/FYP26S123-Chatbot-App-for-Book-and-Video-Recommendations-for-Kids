package com.kidsrec.chatbot.data.model

import com.google.firebase.Timestamp

data class Favorite(
    val id: String = "",
    val userId: String = "",
    val itemId: String = "",
    val type: RecommendationType,
    val title: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val addedAt: Timestamp = Timestamp.now()
)
