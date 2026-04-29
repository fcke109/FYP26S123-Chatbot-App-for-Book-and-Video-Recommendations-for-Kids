package com.kidsrec.chatbot.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude

@Keep
data class Book(

    // basic info
    val id: String = "",
    val title: String = "",
    val author: String = "",

    // age range for kids
    val ageMin: Int = 0,
    val ageMax: Int = 15,

    // main category (the one admin selects)
    val category: String = "",

    // extra categories (NEW)
    // this lets 1 book appear in multiple categories
    // e.g. Animals + Adventure
    val categoryTags: List<String> = emptyList(),

    // general tags (for search/recommendation)
    val tags: List<String> = emptyList(),

    // extra info
    val source: String = "ICDL",
    val language: String = "English",
    val description: String = "",

    // safety + video control
    val isKidSafe: Boolean = true,
    val canPlayInApp: Boolean = false, // only allow safe videos

    // difficulty
    val difficulty: String = "easy",
    val difficultyLevel: String = "easy",

    // book links
    val bookUrl: String = "",
    val readerUrl: String = "",
    val url: String = "",

    // images
    val coverUrl: String = "",
    val imageUrl: String = "",

    // video stuff
    val videoUrl: String = "",
    val youtubeUrl: String = "",
    val isVideo: Boolean = false,
    val type: String = "book",

    // timestamps
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp? = null
) {

    // needed for Firebase
    constructor() : this("")

    // show age nicely in UI
    @get:Exclude
    val ageRating: String
        get() = "$ageMin-$ageMax yrs"

    // pick the best image available
    @get:Exclude
    val displayImageUrl: String
        get() = when {
            coverUrl.isNotBlank() -> coverUrl
            imageUrl.isNotBlank() -> imageUrl
            else -> ""
        }

    // use new difficulty if available
    @get:Exclude
    val effectiveDifficulty: String
        get() = when {
            difficultyLevel.isNotBlank() -> difficultyLevel
            difficulty.isNotBlank() -> difficulty
            else -> "easy"
        }

    // check if this is actually a video
    @get:Exclude
    val isActuallyVideo: Boolean
        get() = isVideo || type.equals("video", ignoreCase = true)

    // decide which URL to use
    @get:Exclude
    val displayUrl: String
        get() = if (isActuallyVideo) {
            // for videos
            when {
                youtubeUrl.isNotBlank() -> youtubeUrl
                videoUrl.isNotBlank() -> videoUrl
                else -> ""
            }
        } else {
            // for books
            when {
                readerUrl.isNotBlank() -> readerUrl
                bookUrl.isNotBlank() -> bookUrl
                url.isNotBlank() -> url
                else -> ""
            }
        }

    // controls what user can actually see
    // videos = stricter rules
    @get:Exclude
    val isVisibleToUser: Boolean
        get() = if (isActuallyVideo) {
            isKidSafe && canPlayInApp && displayUrl.isNotBlank()
        } else {
            title.isNotBlank() && displayUrl.isNotBlank()
        }

    // just returns "book" or "video"
    @get:Exclude
    val contentType: String
        get() = if (isActuallyVideo) "video" else "book"

    // used for search ranking (not saved in firestore)
    @get:Exclude
    @set:Exclude
    var searchScore: Int = 0
}