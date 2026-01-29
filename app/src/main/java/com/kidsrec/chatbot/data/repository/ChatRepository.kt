package com.kidsrec.chatbot.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.kidsrec.chatbot.data.model.ChatMessage
import com.kidsrec.chatbot.data.model.Conversation
import com.kidsrec.chatbot.data.model.MessageRole
import com.kidsrec.chatbot.data.remote.OpenAIMessage
import com.kidsrec.chatbot.data.remote.OpenAIRequest
import com.kidsrec.chatbot.data.remote.OpenAIService
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
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

            // Create system prompt
            val systemPrompt = "You are a friendly children's book recommendation assistant called Book Buddy. " +
                    "Recommend age-appropriate books and educational videos. " +
                    "Be encouraging and make learning fun. Keep responses short (2-3 sentences)."

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

            val botMessage = ChatMessage(
                id = firestore.collection("chatHistory")
                    .document(userId)
                    .collection("conversations")
                    .document(conversationId)
                    .collection("messages")
                    .document().id,
                role = MessageRole.ASSISTANT,
                content = botResponse,
                timestamp = Timestamp.now()
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
}
