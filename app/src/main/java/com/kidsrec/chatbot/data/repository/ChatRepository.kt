package com.kidsrec.chatbot.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.kidsrec.chatbot.data.model.ChatMessage
import com.kidsrec.chatbot.data.model.Conversation
import com.kidsrec.chatbot.data.model.MessageRole
import com.kidsrec.chatbot.data.model.Recommendation
import com.kidsrec.chatbot.data.model.RecommendationType
import com.kidsrec.chatbot.data.remote.OpenAIMessage
import com.kidsrec.chatbot.data.remote.OpenAIRequest
import com.kidsrec.chatbot.data.remote.OpenAIService
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val openAIService: OpenAIService
) {
    suspend fun sendMessage(
        userId: String,
        conversationId: String,
        message: String
    ): Result<ChatMessage> {
        return try {
            // Add user message to Firestore
            val userMessage = ChatMessage(
                id = firestore.collection("chatHistory")
                    .document(userId)
                    .collection("conversations")
                    .document(conversationId)
                    .collection("messages")
                    .document().id,
                role = MessageRole.USER,
                content = message,
                timestamp = Timestamp.now()
            )

            firestore.collection("chatHistory")
                .document(userId)
                .collection("conversations")
                .document(conversationId)
                .collection("messages")
                .document(userMessage.id)
                .set(userMessage)
                .await()

            // Update conversation last updated time
            firestore.collection("chatHistory")
                .document(userId)
                .collection("conversations")
                .document(conversationId)
                .update("lastUpdated", Timestamp.now())
                .await()

            // Get conversation history for context
            val messagesSnapshot = firestore.collection("chatHistory")
                .document(userId)
                .collection("conversations")
                .document(conversationId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .limit(10)
                .get()
                .await()

            val conversationHistory = messagesSnapshot.documents.mapNotNull { doc ->
                val msg = doc.toObject(ChatMessage::class.java)
                msg?.let {
                    OpenAIMessage(
                        role = if (it.role == MessageRole.USER) "user" else "assistant",
                        content = it.content
                    )
                }
            }

            // Create system prompt with JSON format for recommendations
            val systemPrompt = """You are Little Dino, a friendly dinosaur who helps kids discover amazing books and videos! Use simple, fun language.

CRITICAL RULE: You MUST ALWAYS include the [RECOMMENDATIONS] block when suggesting any book, video, or content. Never just mention titles in text - always use the JSON format.

Response format:
1. Write a brief, friendly message (1-2 sentences max)
2. ALWAYS end with recommendations in this EXACT format:

[RECOMMENDATIONS]
[{"type":"BOOK","title":"Exact Book Title","description":"What it's about in 1 sentence","reason":"Why kids will love it"},{"type":"VIDEO","title":"Exact Video Title","description":"What it teaches","reason":"Why it's fun to watch"}]
[/RECOMMENDATIONS]

Rules:
- type must be "BOOK" or "VIDEO" (uppercase)
- Always include 1-3 recommendations
- Keep descriptions short (under 15 words)
- Make reasons exciting for kids
- NEVER skip the [RECOMMENDATIONS] block when suggesting content"""

            // Prepare messages for OpenAI
            val messages = mutableListOf(
                OpenAIMessage(role = "system", content = systemPrompt)
            )
            messages.addAll(conversationHistory)
            messages.add(OpenAIMessage(role = "user", content = message))

            // Call OpenAI API directly
            val openAIRequest = OpenAIRequest(messages = messages)
            val openAIResponse = openAIService.createChatCompletion(openAIRequest)

            val botResponse = openAIResponse.choices.firstOrNull()?.message?.content
                ?: "I'm sorry, I couldn't process that. Can you try again?"

            // Parse recommendations from response
            val (cleanContent, recommendations) = parseRecommendations(botResponse)

            val botMessage = ChatMessage(
                id = firestore.collection("chatHistory")
                    .document(userId)
                    .collection("conversations")
                    .document(conversationId)
                    .collection("messages")
                    .document().id,
                role = MessageRole.ASSISTANT,
                content = cleanContent,
                timestamp = Timestamp.now(),
                recommendations = recommendations
            )

            firestore.collection("chatHistory")
                .document(userId)
                .collection("conversations")
                .document(conversationId)
                .collection("messages")
                .document(botMessage.id)
                .set(botMessage)
                .await()

            Result.success(botMessage)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getMessagesFlow(userId: String, conversationId: String): Flow<List<ChatMessage>> =
        callbackFlow {
            val listener = firestore.collection("chatHistory")
                .document(userId)
                .collection("conversations")
                .document(conversationId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    val messages = snapshot?.toObjects(ChatMessage::class.java) ?: emptyList()
                    trySend(messages)
                }

            awaitClose { listener.remove() }
        }

    suspend fun createConversation(userId: String): Result<String> {
        return try {
            val conversationRef = firestore.collection("chatHistory")
                .document(userId)
                .collection("conversations")
                .document()

            val conversation = Conversation(
                id = conversationRef.id,
                userId = userId,
                createdAt = Timestamp.now(),
                lastUpdated = Timestamp.now()
            )

            conversationRef.set(conversation).await()
            Result.success(conversationRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getConversationsFlow(userId: String): Flow<List<Conversation>> = callbackFlow {
        val listener = firestore.collection("chatHistory")
            .document(userId)
            .collection("conversations")
            .orderBy("lastUpdated", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val conversations = snapshot?.toObjects(Conversation::class.java) ?: emptyList()
                trySend(conversations)
            }

        awaitClose { listener.remove() }
    }

    private fun parseRecommendations(response: String): Pair<String, List<Recommendation>> {
        val recommendations = mutableListOf<Recommendation>()
        var cleanContent = response

        try {
            val startTag = "[RECOMMENDATIONS]"
            val endTag = "[/RECOMMENDATIONS]"

            val startIndex = response.indexOf(startTag)
            val endIndex = response.indexOf(endTag)

            if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
                val jsonString = response.substring(startIndex + startTag.length, endIndex).trim()
                cleanContent = response.substring(0, startIndex).trim()

                val jsonArray = JSONArray(jsonString)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val type = when (obj.optString("type", "BOOK").uppercase()) {
                        "VIDEO" -> RecommendationType.VIDEO
                        else -> RecommendationType.BOOK
                    }
                    recommendations.add(
                        Recommendation(
                            id = UUID.randomUUID().toString(),
                            type = type,
                            title = obj.optString("title", ""),
                            description = obj.optString("description", ""),
                            reason = obj.optString("reason", ""),
                            imageUrl = obj.optString("imageUrl", "")
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // If parsing fails, return original content without recommendations
            cleanContent = response
                .replace(Regex("\\[RECOMMENDATIONS\\].*?\\[/RECOMMENDATIONS\\]", RegexOption.DOT_MATCHES_ALL), "")
                .trim()
        }

        return Pair(cleanContent, recommendations)
    }
}
