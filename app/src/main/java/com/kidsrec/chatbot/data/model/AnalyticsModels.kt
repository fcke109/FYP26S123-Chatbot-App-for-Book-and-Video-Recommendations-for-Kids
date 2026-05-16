package com.kidsrec.chatbot.data.model

// Analytics model for tracking most searched topics
data class TopSearchedTopic(
    val query: String = "",
    val count: Long = 0
)

// Analytics model for tracking most viewed books/videos
data class TopViewedBook(
    val bookId: String = "",
    val bookTitle: String = "",
    val viewCount: Long = 0,
    val count: Long = 0
)

// Analytics model for tracking content drop-off behaviour
data class TopDropOff(
    val itemId: String = "",
    val itemTitle: String = "",
    val avgDurationSeconds: Int = 0,
    val averageDurationSeconds: Int = 0,
    val dropOffCount: Long = 0,
    val count: Long = 0
)