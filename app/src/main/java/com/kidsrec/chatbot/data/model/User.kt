package com.kidsrec.chatbot.data.model

import com.google.firebase.Timestamp

enum class PlanType {
    FREE, PREMIUM, FAMILY, ADMIN
}

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val age: Int = 0,
    // Address removed as requested
    val planType: PlanType = PlanType.FREE,
    val interests: List<String> = emptyList(),
    val readingLevel: String = "Beginner",
    val parentalEmail: String? = null,
    val parentalPin: String? = null,
    val contentFilters: ContentFilters = ContentFilters(),
    val createdAt: Timestamp = Timestamp.now()
)

data class ContentFilters(
    val maxAgeRating: Int = 13,
    val blockedTopics: List<String> = emptyList(),
    val allowVideos: Boolean = true
)
