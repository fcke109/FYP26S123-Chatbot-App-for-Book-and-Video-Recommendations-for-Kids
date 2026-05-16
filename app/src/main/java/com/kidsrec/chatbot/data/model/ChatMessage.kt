package com.kidsrec.chatbot.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

// Represents a single message in the chatbot conversation
@Keep
data class ChatMessage(
    val id: String = "",
    val role: MessageRole = MessageRole.USER,
    val content: String = "",
    val timestamp: Timestamp = Timestamp.now(),

    // Recommendations attached to chatbot replies
    @get:PropertyName("recommendations")
    @set:PropertyName("recommendations")
    var recommendations: List<Recommendation> = emptyList()
) {
    // Empty constructor required for Firestore
    constructor() : this("", MessageRole.USER, "", Timestamp.now(), emptyList())
}
// Defines who sent the message
enum class MessageRole {
    USER,
    ASSISTANT,
    SYSTEM
}

// Represents a full chatbot conversation
@Keep
data class Conversation(
    val id: String = "",
    val userId: String = "",

    // All messages inside the conversation
    val messages: List<ChatMessage> = emptyList(),

    // Short preview text shown in chat history
    val preview: String = "",

    val createdAt: Timestamp = Timestamp.now(),
    val lastUpdated: Timestamp = Timestamp.now()
) {
    // Empty constructor required for Firestore
    constructor() : this("", "", emptyList(), "", Timestamp.now(), Timestamp.now())
}

// Model for chatbot recommendations (books/videos)
@Keep
data class Recommendation(
    val id: String = "",

    // Recommendation type (BOOK or VIDEO)
    @get:PropertyName("type")
    @set:PropertyName("type")
    var type: RecommendationType = RecommendationType.BOOK,

    val title: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val reason: String = "",

    // AI recommendation confidence score
    val relevanceScore: Double = 0.0,

    val url: String = "",

    // Indicates whether content is admin curated
    val isCurated: Boolean = true,

    // Collaborative filtering scores
    val userBasedScore: Double = 0.0,
    val itemBasedScore: Double = 0.0,
    val cfBlendedScore: Double = 0.0
) {
    // Empty constructor required for Firestore
    constructor() : this("", RecommendationType.BOOK, "", "", "", "", 0.0, "", true, 0.0, 0.0, 0.0)
}
// Types of recommendations supported in the app
enum class RecommendationType {
    BOOK,
    VIDEO
}