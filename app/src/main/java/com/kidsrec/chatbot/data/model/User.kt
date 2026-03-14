package com.kidsrec.chatbot.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp

@Keep
enum class PlanType {
    FREE, PREMIUM, ADMIN
}

@Keep
data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val age: Int = 0,
    val planType: PlanType = PlanType.FREE,
    val interests: List<String> = emptyList(),
    val readingLevel: String = "Beginner",
    val parentalEmail: String? = null,
    val parentalPin: String? = null,
    val contentFilters: ContentFilters = ContentFilters(),
    val createdAt: Timestamp = Timestamp.now()
) {
    // No-arg constructor for Firestore
    constructor() : this("")
}

@Keep
data class ContentFilters(
    val maxAgeRating: Int = 13,
    val blockedTopics: List<String> = emptyList(),
    val allowVideos: Boolean = true
) {
    // No-arg constructor for Firestore
    constructor() : this(13, emptyList(), true)
}
