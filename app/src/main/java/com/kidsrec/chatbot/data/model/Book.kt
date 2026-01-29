package com.kidsrec.chatbot.data.model

data class Book(
    val id: String = "",
    val title: String = "",
    val author: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val ageRange: AgeRange = AgeRange.AGES_6_8,
    val genre: String = "",
    val readingLevel: String = "",
    val isbn: String = "",
    val pageCount: Int = 0
)

data class Video(
    val id: String = "",
    val title: String = "",
    val channel: String = "",
    val description: String = "",
    val thumbnailUrl: String = "",
    val videoUrl: String = "",
    val ageRange: AgeRange = AgeRange.AGES_6_8,
    val category: String = "",
    val duration: Int = 0 // in minutes
)

enum class AgeRange(val displayName: String, val minAge: Int, val maxAge: Int) {
    AGES_3_5("Ages 3-5", 3, 5),
    AGES_6_8("Ages 6-8", 6, 8),
    AGES_9_12("Ages 9-12", 9, 12),
    AGES_13_PLUS("Ages 13+", 13, 18)
}
