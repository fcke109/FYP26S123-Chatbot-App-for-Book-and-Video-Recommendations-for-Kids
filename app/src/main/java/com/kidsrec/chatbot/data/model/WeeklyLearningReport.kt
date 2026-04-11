package com.kidsrec.chatbot.data.model

data class WeeklyLearningReport(
    val booksRead: Int = 0,
    val videosWatched: Int = 0,
    val topicsExplored: List<String> = emptyList(),
    val topTopic: String = "",
    val averageReadingLevelScore: Double = 0.0,
    val readingLevelGrowth: String = "Stable",
    val recentEvents: List<LearningProgressEvent> = emptyList()
)