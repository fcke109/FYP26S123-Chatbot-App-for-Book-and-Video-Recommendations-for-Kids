package com.kidsrec.chatbot.data.model

// Model used for collaborative filtering recommendations
data class CFItem(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val url: String = "",

    // Content type (BOOK or VIDEO)
    val type: String = "BOOK",

    val category: String = "",
    val ageMin: Int = 0,
    val ageMax: Int = 18,

    // Safety check for kid-friendly content
    val isKidSafe: Boolean = true,

    // Tags used for recommendation matching
    val tags: List<String> = emptyList()
)