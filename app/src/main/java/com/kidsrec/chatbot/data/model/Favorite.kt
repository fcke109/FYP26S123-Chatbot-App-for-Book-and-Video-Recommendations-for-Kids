package com.kidsrec.chatbot.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

@Keep
data class Favorite(
    val id: String = "",
    val userId: String = "",
    val itemId: String = "",
    @get:PropertyName("type")
    @set:PropertyName("type")
    var type: RecommendationType = RecommendationType.BOOK,
    val title: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val addedAt: Timestamp = Timestamp.now()
) {
    // No-arg constructor for Firestore
    constructor() : this("", "", "", RecommendationType.BOOK, "", "", "", Timestamp.now())
}
