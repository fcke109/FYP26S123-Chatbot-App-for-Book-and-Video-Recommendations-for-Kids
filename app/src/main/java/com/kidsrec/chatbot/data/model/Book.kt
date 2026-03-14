package com.kidsrec.chatbot.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude

/**
 * Official Book Model for content_books
 */
@Keep
data class Book(
    val id: String = "",
    val title: String = "",
    val author: String = "",
    val ageMin: Int = 0,
    val ageMax: Int = 15,
    val category: String = "",
    val source: String = "ICDL",
    val language: String = "English",
    val description: String = "",
    val tags: List<String> = emptyList(),
    val isKidSafe: Boolean = true,
    val difficulty: String = "easy",
    val bookUrl: String = "",
    val readerUrl: String = "",
    val coverUrl: String = "",
    val createdAt: Timestamp = Timestamp.now()
) {
    // Computed property for UI
    @get:Exclude
    val ageRating: String get() = "$ageMin-$ageMax yrs"

    // Search score (non-persisted)
    @get:Exclude @set:Exclude
    var searchScore: Int = 0
}
