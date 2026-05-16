package com.kidsrec.chatbot.data.model

// Stores weekly learning analytics and progress summary for a child user
data class WeeklyLearningReport(

    // Total learning activities completed this week
    val booksRead: Int = 0,
    val videosWatched: Int = 0,

    // Topics explored by the user
    val topicsExplored: List<String> = emptyList(),
    val topTopic: String = "",
    val averageReadingLevelScore: Double = 0.0,
    val readingLevelGrowth: String = "Stable",
    val recentEvents: List<LearningProgressEvent> = emptyList()
)