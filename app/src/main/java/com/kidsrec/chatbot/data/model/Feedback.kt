package com.kidsrec.chatbot.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp

@Keep
data class Feedback(
    val id: String = "",
    val userId: String = "",
    val recommendationId: String = "",
    val recommendationTitle: String = "",
    val recommendationType: RecommendationType = RecommendationType.BOOK,
    val isPositive: Boolean = true,
    val timestamp: Timestamp = Timestamp.now()
)
