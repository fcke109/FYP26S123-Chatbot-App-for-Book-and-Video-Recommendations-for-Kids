package com.kidsrec.chatbot.data.model

// Starter books automatically assigned to new child accounts
data class StarterBookSeed(
    val id: String = "",
    val title: String = "",
    val author: String = "",

    // Book image and reading links
    val coverUrl: String = "",
    val bookUrl: String = "",
    val readerUrl: String = "",

    // Source/provider of the book
    val source: String = "",

    // Recommended age range
    val ageMin: Int = 3,
    val ageMax: Int = 12
)