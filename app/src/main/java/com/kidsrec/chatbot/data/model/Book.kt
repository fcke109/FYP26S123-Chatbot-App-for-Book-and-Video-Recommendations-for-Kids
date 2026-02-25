package com.kidsrec.chatbot.data.model

import com.google.firebase.Timestamp

data class Book(
    val id: String = "",
    val title: String = "",
    val author: String = "",
    val coverUrl: String? = null,
    val description: String? = null,
    val ageRating: String = "6-8 years",
    val readingAvailability: String = "Intermediate",
    
    // Support for Visual Picture Books
    val pageUrls: List<String> = emptyList(), 
    val isPictureBook: Boolean = true,

    // Reading links
    val readerUrl: String? = null,
    val openLibraryUrl: String? = null,
    val gutenbergHtmlUrl: String? = null,
    val gutenbergTxtUrl: String? = null,
    
    val createdAt: Timestamp = Timestamp.now()
)

data class Video(
    val id: String = "",
    val title: String = "",
    val channel: String = "",
    val description: String = "",
    val thumbnailUrl: String = "",
    val videoUrl: String = "",
    val ageRange: String = "Ages 6-8",
    val category: String = "",
    val duration: Int = 0
)
