package com.kidsrec.chatbot.data.model

data class StarterBookSeed(
    val id: String = "",
    val title: String = "",
    val author: String = "",
    val coverUrl: String = "",
    val bookUrl: String = "",
    val readerUrl: String = "",
    val source: String = "",
    val ageMin: Int = 3,
    val ageMax: Int = 12
)