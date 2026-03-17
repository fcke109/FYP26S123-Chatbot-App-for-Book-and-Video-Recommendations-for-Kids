package com.kidsrec.chatbot.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude

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
    val canPlayInApp: Boolean = false, // New field to filter playable videos

    val difficulty: String = "easy",
    val difficultyLevel: String = "easy",

    val bookUrl: String = "",
    val readerUrl: String = "",
    val url: String = "",

    val coverUrl: String = "",
    val imageUrl: String = "",

    val videoUrl: String = "",
    val youtubeUrl: String = "",
    val isVideo: Boolean = false,
    val type: String = "book",

    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp? = null
) {
    constructor() : this("")

    @get:Exclude
    val ageRating: String
        get() = "$ageMin-$ageMax yrs"

    @get:Exclude
    val displayImageUrl: String
        get() = when {
            coverUrl.isNotBlank() -> coverUrl
            imageUrl.isNotBlank() -> imageUrl
            else -> ""
        }

    @get:Exclude
    val effectiveDifficulty: String
        get() = when {
            difficultyLevel.isNotBlank() -> difficultyLevel
            difficulty.isNotBlank() -> difficulty
            else -> "easy"
        }

    @get:Exclude
    val isActuallyVideo: Boolean
        get() = isVideo || type.equals("video", ignoreCase = true)

    @get:Exclude
    val displayUrl: String
        get() = if (isActuallyVideo) {
            when {
                youtubeUrl.isNotBlank() -> youtubeUrl
                videoUrl.isNotBlank() -> videoUrl
                else -> ""
            }
        } else {
            when {
                readerUrl.isNotBlank() -> readerUrl
                bookUrl.isNotBlank() -> bookUrl
                url.isNotBlank() -> url
                else -> ""
            }
        }

    /**
     * Logic to determine if this content should be visible to users.
     * Books are shown normally.
     * Videos are ONLY shown if they are kid-safe, have a URL, and can play in-app.
     */
    @get:Exclude
    val isVisibleToUser: Boolean
        get() = if (isActuallyVideo) {
            isKidSafe && canPlayInApp && displayUrl.isNotBlank()
        } else {
            // It's a book - show if it has basic data
            title.isNotBlank() && displayUrl.isNotBlank()
        }

    @get:Exclude
    val contentType: String
        get() = if (isActuallyVideo) "video" else "book"

    @get:Exclude
    @set:Exclude
    var searchScore: Int = 0
}