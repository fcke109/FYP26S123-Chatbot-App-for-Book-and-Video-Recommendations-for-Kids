package com.kidsrec.chatbot.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

// Stores user feedback for recommendations shown by the chatbot
@Keep
data class Feedback(
    val id: String = "",
    val userId: String = "",

    // Recommended content details
    val recommendationId: String = "",
    val recommendationTitle: String = "",
    val recommendationType: RecommendationType = RecommendationType.BOOK,

    // Indicates whether the feedback was positive or negative
    @get:PropertyName("isPositive")
    @set:PropertyName("isPositive")
    var isPositive: Boolean = true,

    // Time feedback was submitted
    val timestamp: Timestamp = Timestamp.now()
)
