package com.kidsrec.chatbot.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

@Keep
data class Feedback(
    val id: String = "",
    val userId: String = "",
    val recommendationId: String = "",
    val recommendationTitle: String = "",
    val recommendationType: RecommendationType = RecommendationType.BOOK,
    @get:PropertyName("isPositive")
    @set:PropertyName("isPositive")
    var isPositive: Boolean = true,
    val timestamp: Timestamp = Timestamp.now()
)
