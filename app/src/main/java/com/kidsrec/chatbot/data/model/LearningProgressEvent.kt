package com.kidsrec.chatbot.data.model

import com.google.firebase.Timestamp

data class LearningProgressEvent(
    val id: String = "",
    val childUserId: String = "",
    val type: String = "", // BOOK_READ, VIDEO_WATCHED, TOPIC_EXPLORED
    val title: String = "",
    val topic: String = "",
    val contentId: String = "",
    val contentType: String = "", // BOOK, VIDEO, TOPIC
    val readingLevel: String = "", // Beginner, Intermediate, Advanced
    val timestamp: Timestamp = Timestamp.now(),
    val durationSeconds: Long = 0L
)