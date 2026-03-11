package com.kidsrec.chatbot.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.kidsrec.chatbot.data.model.Book
import com.kidsrec.chatbot.data.model.ChatMessage
import com.kidsrec.chatbot.data.model.Conversation
import com.kidsrec.chatbot.data.model.Favorite
import com.kidsrec.chatbot.data.model.MessageRole
import com.kidsrec.chatbot.data.model.Recommendation
import com.kidsrec.chatbot.data.model.RecommendationType
import com.kidsrec.chatbot.data.model.User
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

/**
 * ChatDataManager: Manages chatbot conversations, message storage, and AI recommendations.
 */
@Singleton
class ChatDataManager @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val openAIService: OpenAIService,
    private val bookDataManager: BookDataManager,
    private val recommendationEngine: RecommendationEngine,
    private val accountManager: AccountManager,
    private val favoritesManager: FavoritesManager
) {
    suspend fun sendMessage(
        userId: String,
        conversationId: String,
        message: String
    ): Result<ChatMessage> {
        return try {
            val curatedBooks = bookDataManager.getCuratedBooks().getOrDefault(emptyList())
            val curatedBooksContext = if (curatedBooks.isNotEmpty()) {
                "Available curated books to suggest: " + 
                curatedBooks.joinToString(", ") { "${it.title} by ${it.author}" }
            } else ""

            val userMessage = ChatMessage(
                id = firestore.collection("chatHistory").document(userId).collection("conversations").document(conversationId).collection("messages").document().id,
                role = MessageRole.USER,
                content = message,
                timestamp = Timestamp.now()
            )

            firestore.collection("chatHistory").document(userId).collection("conversations").document(conversationId).collection("messages").document(userMessage.id).set(userMessage).await()

            // Update conversation metadata for chat history
            firestore.collection("chatHistory").document(userId).collection("conversations").document(conversationId)
                .update(mapOf("lastUpdated" to Timestamp.now(), "preview" to message.take(80))).await()

            val messagesSnapshot = firestore.collection("chatHistory")
                .document(userId)
                .collection("conversations")
                .document(conversationId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .limit(12)
                .get()
                .await()

            val conversationHistory = messagesSnapshot.documents.mapNotNull { doc ->
                val msg = doc.toObject(ChatMessage::class.java)
                msg?.let { OpenAIMessage(role = if (it.role == MessageRole.USER) "user" else "assistant", content = it.content) }
            }

            val systemPrompt = """You are Little Dino, a friendly dinosaur helping kids find books and videos!

$curatedBooksContext

CRITICAL: You MUST provide a mix of BOTH "BOOK" and "VIDEO" recommendations in every response.
- Use "BOOK" for the curated books listed above or famous children's classics.
- Use "VIDEO" for fun educational songs, nursery rhymes, or "YouTube Kids" style animated stories.

Response format:
1. Friendly message (1-2 sentences).
2. End with this EXACT block:
[RECOMMENDATIONS]
[{"type":"BOOK","title":"Book Name","description":"1 sentence desc","reason":"Why fun"},{"type":"VIDEO","title":"Video Name","description":"Fun animated story","reason":"Great to watch","url":"https://www.youtube.com/watch?v=VIDEO_ID"}]
[/RECOMMENDATIONS]

RULES:
- 'type' MUST be "BOOK" or "VIDEO".
- Include at least ONE VIDEO and ONE BOOK if possible.
- For VIDEO: include a "url" field with the direct YouTube watch link (https://www.youtube.com/watch?v=...) if you know it. If unsure, omit the url field.
- For BOOK: do NOT include a url field (we handle book links internally).
- Videos should be suitable for kids."""

            val messages = mutableListOf(OpenAIMessage(role = "system", content = systemPrompt))
            messages.addAll(conversationHistory)
            messages.add(OpenAIMessage(role = "user", content = message))

            val openAIResponse = openAIService.createChatCompletion(OpenAIRequest(messages = messages))
            val botResponse = openAIResponse.choices.firstOrNull()?.message?.content ?: "Let's find some stories!"

            val (cleanContent, parsedRecs) = parseRecommendations(botResponse)
            val withUrls = attachBookUrls(parsedRecs, curatedBooks)
            val recommendations = scoreWithANN(withUrls, curatedBooks, userId)

            val botMessage = ChatMessage(
                id = firestore.collection("chatHistory").document(userId).collection("conversations").document(conversationId).collection("messages").document().id,
                role = MessageRole.ASSISTANT,
                content = cleanContent,
                timestamp = Timestamp.now(),
                recommendations = recommendations
            )

            firestore.collection("chatHistory").document(userId).collection("conversations").document(conversationId).collection("messages").document(botMessage.id).set(botMessage).await()
            Result.success(botMessage)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getMessagesFlow(userId: String, conversationId: String): Flow<List<ChatMessage>> = callbackFlow {
        val listener = firestore.collection("chatHistory").document(userId).collection("conversations").document(conversationId).collection("messages").orderBy("timestamp", Query.Direction.ASCENDING).addSnapshotListener { snapshot, _ ->
            snapshot?.let { trySend(it.toObjects(ChatMessage::class.java)) }
        }
        awaitClose { listener.remove() }
    }

    suspend fun createConversation(userId: String): Result<String> {
        return try {
            val ref = firestore.collection("chatHistory").document(userId).collection("conversations").document()
            ref.set(Conversation(id = ref.id, userId = userId)).await()
            Result.success(ref.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getConversationsFlow(userId: String, limit: Int = 20): Flow<List<Conversation>> = callbackFlow {
        val listener = firestore.collection("chatHistory")
            .document(userId)
            .collection("conversations")
            .orderBy("lastUpdated", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
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

            if (startIndex != -1 && endIndex != -1) {
                val jsonString = response.substring(startIndex + startTag.length, endIndex).trim()
                cleanContent = response.substring(0, startIndex).trim()
                val jsonArray = JSONArray(jsonString)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val type = if (obj.optString("type").uppercase() == "VIDEO") RecommendationType.VIDEO else RecommendationType.BOOK
                    recommendations.add(Recommendation(
                        id = UUID.randomUUID().toString(),
                        type = type,
                        title = obj.optString("title"),
                        description = obj.optString("description"),
                        reason = obj.optString("reason"),
                        url = obj.optString("url", "")
                    ))
                }
            }
        } catch (e: Exception) { /* Silent fallback */ }
        return Pair(cleanContent, recommendations)
    }

    /**
     * Score each recommendation using the ANN engine for the current user.
     */
    private suspend fun scoreWithANN(
        recommendations: List<Recommendation>,
        curatedBooks: List<Book>,
        userId: String
    ): List<Recommendation> {
        return try {
            val userDoc = firestore.collection("users").document(userId).get().await()
            val user = userDoc.toObject(User::class.java) ?: return recommendations
            val favorites = favoritesManager.getFavorites(userId)

            recommendations.map { rec ->
                val matchingBook = curatedBooks.firstOrNull { book ->
                    book.title.contains(rec.title, ignoreCase = true) ||
                        rec.title.contains(book.title, ignoreCase = true)
                }
                val score = if (matchingBook != null) {
                    recommendationEngine.scoreBook(matchingBook, user, favorites)
                } else {
                    recommendationEngine.scoreRecommendation(rec, user, favorites)
                }
                rec.copy(relevanceScore = score)
            }.sortedByDescending { it.relevanceScore }
        } catch (e: Exception) {
            recommendations
        }
    }

    /**
     * Match BOOK recommendations against curated books and attach direct reader URLs.
     */
    private fun attachBookUrls(
        recommendations: List<Recommendation>,
        curatedBooks: List<Book>
    ): List<Recommendation> {
        if (curatedBooks.isEmpty()) return recommendations
        return recommendations.map { rec ->
            if (rec.type == RecommendationType.BOOK && rec.url.isBlank()) {
                val matchingBook = curatedBooks.firstOrNull { book ->
                    book.title.contains(rec.title, ignoreCase = true) ||
                        rec.title.contains(book.title, ignoreCase = true)
                }
                if (matchingBook != null) {
                    val bookUrl = matchingBook.readerUrl.ifBlank { matchingBook.bookUrl }
                    rec.copy(
                        url = bookUrl,
                        imageUrl = rec.imageUrl.ifBlank { matchingBook.coverUrl }
                    )
                } else rec
            } else rec
        }
    }
}
