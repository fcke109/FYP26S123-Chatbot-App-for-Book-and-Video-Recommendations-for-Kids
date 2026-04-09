package com.kidsrec.chatbot.data.model

data class TopSearchedTopic(
    val query: String = "",
    val count: Long = 0
)

data class TopViewedBook(
    val bookId: String = "",
    val bookTitle: String = "",
    val viewCount: Long = 0,
    val count: Long = 0
)

data class TopDropOff(
    val itemId: String = "",
    val itemTitle: String = "",
    val avgDurationSeconds: Int = 0,
    val averageDurationSeconds: Int = 0,
    val dropOffCount: Long = 0,
    val count: Long = 0
)
