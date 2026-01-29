package com.kidsrec.chatbot.data.model

import com.google.firebase.Timestamp

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val age: Int = 0,
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
