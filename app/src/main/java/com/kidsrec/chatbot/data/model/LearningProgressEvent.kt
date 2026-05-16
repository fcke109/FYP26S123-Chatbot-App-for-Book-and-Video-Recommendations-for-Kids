package com.kidsrec.chatbot.data.model

import com.google.firebase.Timestamp

// Tracks learning and activity progress for child users
data class LearningProgressEvent(
    val id: String = "",
    val childUserId: String = "",

    // Activity type (BOOK_READ, VIDEO_WATCHED, TOPIC_EXPLORED)
    val type: String = "",

    val title: String = "",
    val topic: String = "",

    // Related content details
    val contentId: String = "",
    val contentType: String = "",

    // Difficulty/reading level of the content
    val readingLevel: String = "",

    val timestamp: Timestamp = Timestamp.now(),

    // Time spent on the activity
    val durationSeconds: Long = 0L
)