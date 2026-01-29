package com.kidsrec.chatbot.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

@Keep
data class ChatMessage(
    val id: String = "",
    val role: MessageRole = MessageRole.USER,
    val content: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    @get:PropertyName("recommendations")
    @set:PropertyName("recommendations")
    var recommendations: List<Recommendation> = emptyList()
) {
    // No-arg constructor for Firestore
    constructor() : this("", MessageRole.USER, "", Timestamp.now(), emptyList())
}

enum class MessageRole {
    USER,
    ASSISTANT,
    SYSTEM
}

@Keep
data class Conversation(
    val id: String = "",
    val userId: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val createdAt: Timestamp = Timestamp.now(),
    val lastUpdated: Timestamp = Timestamp.now()
) {
    // No-arg constructor for Firestore
    constructor() : this("", "", emptyList(), Timestamp.now(), Timestamp.now())
}

@Keep
data class Recommendation(
    val id: String = "",
    @get:PropertyName("type")
    @set:PropertyName("type")
    var type: RecommendationType = RecommendationType.BOOK,
    val title: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val reason: String = ""
) {
    // No-arg constructor for Firestore
    constructor() : this("", RecommendationType.BOOK, "", "", "", "")
}

enum class RecommendationType {
    BOOK,
    VIDEO
}
