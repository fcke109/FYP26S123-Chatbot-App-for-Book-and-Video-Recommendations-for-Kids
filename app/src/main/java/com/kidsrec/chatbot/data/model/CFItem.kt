package com.kidsrec.chatbot.data.model

data class CFItem(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val url: String = "",
    val type: String = "BOOK",
    val category: String = "",
    val ageMin: Int = 0,
    val ageMax: Int = 18,
    val isKidSafe: Boolean = true,
    val tags: List<String> = emptyList()
)