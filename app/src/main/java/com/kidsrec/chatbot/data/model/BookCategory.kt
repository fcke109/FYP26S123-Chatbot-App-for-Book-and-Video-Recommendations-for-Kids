package com.kidsrec.chatbot.data.model

data class BookCategory(

    // unique id from Firestore
    val id: String = "",

    // category name shown in UI
    val name: String = "",

    // short description
    val description: String = "",

    // related words for smarter matching
    // e.g. Animals = pets, wildlife, jungle, dog, cat
    val tags: List<String> = emptyList()
)