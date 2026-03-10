package com.kidsrec.chatbot.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude

/**
 * Official Book Model for content_books
 * Every book is numbered starting from 001
 */
data class Book(
    val id: String = "",           // e.g., 001, 002...
    val title: String = "",
    val author: String = "",
    val ageMin: Int = 0,
    val ageMax: Int = 15,
    val category: String = "",     // e.g., Adventure, Animal Stories
    val source: String = "ICDL",
    val language: String = "English",
    val description: String = "",
    val tags: List<String> = emptyList(),
    val isKidSafe: Boolean = true,
    val difficulty: String = "easy", // easy, medium, hard
    val bookUrl: String = "",      // Interactive URL (no preview pages)
    val readerUrl: String = "",    // Direct reader URL
    val coverUrl: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    
    @get:Exclude @set:Exclude
    var searchScore: Int = 0       // Match percentage for search results
) {
    val ageRating: String get() = "$ageMin-$ageMax yrs"
}
