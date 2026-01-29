package com.kidsrec.chatbot.data.model

import com.google.firebase.Timestamp

data class ChatMessage(
    val id: String = "",
    val role: MessageRole = MessageRole.USER,
    val content: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val recommendations: List<Recommendation> = emptyList()
)

enum class MessageRole {
    USER,
    ASSISTANT,
    SYSTEM
}

data class Conversation(
    val id: String = "",
    val userId: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val createdAt: Timestamp = Timestamp.now(),
    val lastUpdated: Timestamp = Timestamp.now()
)

data class Recommendation(
    val id: String = "",
    val type: RecommendationType,
    val title: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val reason: String = ""
)

enum class RecommendationType {
    BOOK,
    VIDEO
}
