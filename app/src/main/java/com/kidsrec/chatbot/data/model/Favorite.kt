package com.kidsrec.chatbot.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

// Stores books/videos saved by users as favourites
@Keep
data class Favorite(
    val id: String = "",
    val userId: String = "",
    val itemId: String = "",

    // Favourite content type (BOOK or VIDEO)
    @get:PropertyName("type")
    @set:PropertyName("type")
    var type: RecommendationType = RecommendationType.BOOK,
    val title: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val url: String = "",

    // Timestamp when item was added to favourites
    val addedAt: Timestamp = Timestamp.now()
) {
    // Empty constructor required for Firestore
    constructor() : this("", "", "", RecommendationType.BOOK, "", "", "", "", Timestamp.now())
}
