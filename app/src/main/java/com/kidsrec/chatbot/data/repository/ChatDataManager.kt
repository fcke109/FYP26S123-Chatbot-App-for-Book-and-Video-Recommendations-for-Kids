package com.kidsrec.chatbot.data.repository

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.kidsrec.chatbot.data.model.Book
import com.kidsrec.chatbot.data.model.ChatMessage
import com.kidsrec.chatbot.data.model.Conversation
import com.kidsrec.chatbot.data.model.MessageRole
import com.kidsrec.chatbot.data.model.Recommendation
import com.kidsrec.chatbot.data.model.RecommendationType
import com.kidsrec.chatbot.data.model.User
import com.kidsrec.chatbot.data.remote.GeminiService
import com.kidsrec.chatbot.data.remote.OpenAIMessage
import com.kidsrec.chatbot.data.remote.OpenAIRequest
import com.kidsrec.chatbot.data.remote.OpenAIService
import com.kidsrec.chatbot.data.remote.OpenLibraryService
import com.kidsrec.chatbot.data.remote.YouTubeService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatDataManager @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val openAIService: OpenAIService,
    private val geminiService: GeminiService,
    private val bookDataManager: BookDataManager,
    private val recommendationEngine: RecommendationEngine,
    private val accountManager: AccountManager,
    private val favoritesManager: FavoritesManager,
    private val openLibraryService: OpenLibraryService,
    private val youTubeService: YouTubeService
) {

    private data class ApprovedVideo(
        val id: String,
        val title: String,
        val description: String,
        val reason: String,
        val url: String,
        val imageUrl: String,
        val tags: List<String> = emptyList()
    )

    private val approvedVideos = listOf(
        ApprovedVideo(
            id = "vid_abc_song",
            title = "ABC Song for Kids",
            description = "A cheerful alphabet song for early learners.",
            reason = "Great for learning letters in a fun way.",
            url = "https://www.youtube.com/watch?v=75p-N9YKqNo",
            imageUrl = "https://img.youtube.com/vi/75p-N9YKqNo/hqdefault.jpg",
            tags = listOf("alphabet", "abc", "letters", "phonics", "preschool")
        ),
        ApprovedVideo(
            id = "vid_counting_song",
            title = "Counting Song for Children",
            description = "A fun counting video that helps kids learn numbers.",
            reason = "Helps with basic counting and number recognition.",
            url = "https://www.youtube.com/watch?v=DR-cfDsHCGA",
            imageUrl = "https://img.youtube.com/vi/DR-cfDsHCGA/hqdefault.jpg",
            tags = listOf("counting", "numbers", "math", "preschool")
        ),
        ApprovedVideo(
            id = "vid_old_macdonald",
            title = "Old MacDonald Had a Farm",
            description = "A classic nursery rhyme with animals and fun sounds.",
            reason = "Fun animal song that kids usually enjoy.",
            url = "https://www.youtube.com/watch?v=_6HzoUcx3eo",
            imageUrl = "https://img.youtube.com/vi/_6HzoUcx3eo/hqdefault.jpg",
            tags = listOf("animals", "nursery rhyme", "farm", "song")
        ),
        ApprovedVideo(
            id = "vid_twinkle",
            title = "Twinkle Twinkle Little Star",
            description = "A gentle nursery rhyme for young children.",
            reason = "Calm and familiar song for little kids.",
            url = "https://www.youtube.com/watch?v=yCjJyiqpAuU",
            imageUrl = "https://img.youtube.com/vi/yCjJyiqpAuU/hqdefault.jpg",
            tags = listOf("nursery rhyme", "bedtime", "song", "star")
        ),
        ApprovedVideo(
            id = "vid_shapes_song",
            title = "Shapes Song for Kids",
            description = "A bright and fun song introducing simple shapes.",
            reason = "Good for learning circles, squares, triangles and more.",
            url = "https://www.youtube.com/watch?v=OEbRDtCAFdU",
            imageUrl = "https://img.youtube.com/vi/OEbRDtCAFdU/hqdefault.jpg",
            tags = listOf("shapes", "learning", "preschool", "geometry")
        ),
        ApprovedVideo(
            id = "vid_colors_song",
            title = "Colors Song for Children",
            description = "A colorful video that teaches basic colors.",
            reason = "Makes color learning easy and fun.",
            url = "https://www.youtube.com/watch?v=SLZcWGQQsmg",
            imageUrl = "https://img.youtube.com/vi/SLZcWGQQsmg/hqdefault.jpg",
            tags = listOf("colors", "learning", "preschool")
        ),
        ApprovedVideo(
            id = "vid_baby_shark",
            title = "Baby Shark Dance",
            description = "The famous Baby Shark song with fun dance moves.",
            reason = "A super catchy and fun song for kids!",
            url = "https://www.youtube.com/watch?v=XqZsoesa55w",
            imageUrl = "https://img.youtube.com/vi/XqZsoesa55w/hqdefault.jpg",
            tags = listOf("baby shark", "shark", "dance", "song", "ocean", "sea", "fish", "pinkfong")
        ),
        ApprovedVideo(
            id = "vid_wheels_bus",
            title = "Wheels on the Bus",
            description = "A classic children's song about a fun bus ride.",
            reason = "A fun sing-along about riding the bus.",
            url = "https://www.youtube.com/watch?v=e_04ZrNroTo",
            imageUrl = "https://img.youtube.com/vi/e_04ZrNroTo/hqdefault.jpg",
            tags = listOf("bus", "wheels", "nursery rhyme", "song", "transport", "vehicles")
        ),
        ApprovedVideo(
            id = "vid_solar_system",
            title = "Solar System for Kids",
            description = "Learn about all the planets in our solar system.",
            reason = "A great way to explore space and planets.",
            url = "https://www.youtube.com/watch?v=Qd6nLM2QlWw",
            imageUrl = "https://img.youtube.com/vi/Qd6nLM2QlWw/hqdefault.jpg",
            tags = listOf("space", "planets", "solar system", "science", "earth", "mars", "jupiter")
        ),
        ApprovedVideo(
            id = "vid_dinosaurs",
            title = "Dinosaurs for Kids",
            description = "Learn about different types of dinosaurs.",
            reason = "Explore the amazing world of dinosaurs!",
            url = "https://www.youtube.com/watch?v=GQER4yliMQQ",
            imageUrl = "https://img.youtube.com/vi/GQER4yliMQQ/hqdefault.jpg",
            tags = listOf("dinosaur", "dinosaurs", "t-rex", "science", "prehistoric", "animals")
        ),
        ApprovedVideo(
            id = "vid_ocean_animals",
            title = "Ocean Animals for Kids",
            description = "Discover amazing creatures that live under the sea.",
            reason = "Learn about whales, dolphins, and more!",
            url = "https://www.youtube.com/watch?v=aYAjdShvWEk",
            imageUrl = "https://img.youtube.com/vi/aYAjdShvWEk/hqdefault.jpg",
            tags = listOf("ocean", "sea", "animals", "whale", "dolphin", "fish", "marine", "nature")
        ),
        ApprovedVideo(
            id = "vid_phonics",
            title = "Phonics Song for Kids",
            description = "Learn letter sounds with this fun phonics song.",
            reason = "Great for early reading skills!",
            url = "https://www.youtube.com/watch?v=BELlZKpi1Zs",
            imageUrl = "https://img.youtube.com/vi/BELlZKpi1Zs/hqdefault.jpg",
            tags = listOf("phonics", "reading", "letters", "alphabet", "learning", "school")
        ),
        ApprovedVideo(
            id = "vid_five_senses",
            title = "Five Senses for Kids",
            description = "Learn about your five senses: sight, hearing, touch, taste, and smell.",
            reason = "Discover how your body works!",
            url = "https://www.youtube.com/watch?v=q1xNuU7gaAQ",
            imageUrl = "https://img.youtube.com/vi/q1xNuU7gaAQ/hqdefault.jpg",
            tags = listOf("senses", "body", "science", "health", "learning")
        )
    )

    suspend fun sendMessage(
        userId: String,
        conversationId: String,
        message: String
    ): Result<ChatMessage> {
        return try {
            // Sanitize and validate input
            val validationError = com.kidsrec.chatbot.util.InputSanitizer.validateMessage(message)
            if (validationError != null) {
                return Result.failure(Exception(validationError))
            }
            val sanitizedMessage = com.kidsrec.chatbot.util.InputSanitizer.sanitizeChatMessage(message)

            val curatedBooks = bookDataManager.getCuratedBooks().getOrDefault(emptyList())

            val curatedBooksContext = if (curatedBooks.isNotEmpty()) {
                buildString {
                    appendLine("Available curated books:")
                    curatedBooks.forEachIndexed { index, book ->
                        appendLine("${index + 1}. ${book.title} by ${book.author}")
                    }
                }
            } else {
                "No curated books are currently available."
            }

            val approvedVideosContext = if (approvedVideos.isNotEmpty()) {
                buildString {
                    appendLine("Available approved kid-safe videos:")
                    approvedVideos.forEachIndexed { index, video ->
                        appendLine("${index + 1}. ${video.title} - ${video.description}")
                    }
                }
            } else {
                "No approved videos are currently available."
            }

            val userMessage = ChatMessage(
                id = firestore.collection("chatHistory")
                    .document(userId)
                    .collection("conversations")
                    .document(conversationId)
                    .collection("messages")
                    .document().id,
                role = MessageRole.USER,
                content = sanitizedMessage,
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

            firestore.collection("chatHistory")
                .document(userId)
                .collection("conversations")
                .document(conversationId)
                .update(
                    mapOf(
                        "lastUpdated" to Timestamp.now(),
                        "preview" to message.take(80)
                    )
                )
                .await()

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
                msg?.let {
                    OpenAIMessage(
                        role = if (it.role == MessageRole.USER) "user" else "assistant",
                        content = it.content
                    )
                }
            }

            val systemPrompt = """
You are Little Dino, a friendly dinosaur helping kids find books and videos.

$curatedBooksContext

$approvedVideosContext

CRITICAL RULES:
1. You may recommend books. Prefer curated books when relevant, but if the child asks about a specific topic (e.g. "baby shark", "peppa pig", "dinosaurs"), you SHOULD also recommend a book closely matching their topic, even if it is not in the curated list.
2. You may recommend videos. Prefer approved videos when relevant, but if the child asks about a specific topic, recommend a video matching what they asked for, even if it is not in the approved list.
3. For any recommendation, provide a reason why it is fun for the child.
4. Always include a mix of BOTH:
   - at least 1 BOOK
   - at least 1 VIDEO
5. Keep the response friendly and short for children.

Response format:
1. Friendly message (1-2 sentences).
2. End with this EXACT block:

[RECOMMENDATIONS]
[
  {"type":"BOOK","title":"Exact Book Title","description":"1 short sentence","reason":"Why it is fun"},
  {"type":"VIDEO","title":"Video Title","description":"1 short sentence","reason":"Why it is fun"}
]
[/RECOMMENDATIONS]

RULES FOR JSON:
- type must be BOOK or VIDEO
- for BOOK, if a curated book matches the child's interest, use that exact title. Otherwise, use a real, well-known children's book title related to what they asked for (e.g. for "baby shark", suggest "Baby Shark" by Pinkfong or a similar real book).
- for VIDEO, if the title matches an approved video, use that exact title. Otherwise, use a clear searchable title closely matching what the child asked for (e.g. "Baby Shark Dance" not a generic title).
- do NOT include url
- do NOT include imageUrl
- keep descriptions short
""".trimIndent()

            val messagesList = mutableListOf(
                OpenAIMessage(role = "system", content = systemPrompt)
            )
            messagesList.addAll(conversationHistory)
            messagesList.add(OpenAIMessage(role = "user", content = sanitizedMessage))

            // Use Gemini (free tier) as primary, OpenAI as fallback
            val rawResponse = try {
                geminiService.chat(systemPrompt, conversationHistory, sanitizedMessage)
            } catch (e: Exception) {
                Log.w("ChatDataManager", "Gemini failed, trying OpenAI: ${e.message}")
                val openAIResponse = openAIService.createChatCompletion(
                    OpenAIRequest(messages = messagesList)
                )
                openAIResponse.choices.firstOrNull()?.message?.content
                    ?: "Let's find some fun stories and videos!"
            }

            // Filter response for inappropriate content (defense-in-depth)
            val botResponse = com.kidsrec.chatbot.util.ContentFilter.sanitizeResponse(rawResponse)

            val (cleanContent, parsedRecs) = parseRecommendations(botResponse)

            val withContentUrls = attachContentUrls(
                recommendations = parsedRecs,
                curatedBooks = curatedBooks,
                approvedVideos = approvedVideos
            )

            val ensuredMix = ensureBookAndVideoMix(
                originalMessage = sanitizedMessage,
                recommendations = withContentUrls,
                curatedBooks = curatedBooks,
                approvedVideos = approvedVideos
            )

            val recommendations = scoreWithANN(
                recommendations = ensuredMix,
                curatedBooks = curatedBooks,
                userId = userId
            )

            val botMessage = ChatMessage(
                id = firestore.collection("chatHistory")
                    .document(userId)
                    .collection("conversations")
                    .document(conversationId)
                    .collection("messages")
                    .document().id,
                role = MessageRole.ASSISTANT,
                content = cleanContent.ifBlank { "Here are some fun picks for you!" },
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
            Log.e("ChatDataManager", "sendMessage failed: ${e.javaClass.simpleName}: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun getMessagesFlow(userId: String, conversationId: String): Flow<List<ChatMessage>> = callbackFlow {
        val listener = firestore.collection("chatHistory")
            .document(userId)
            .collection("conversations")
            .document(conversationId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ChatDataManager", "Error loading messages: ${error.message}")
                    return@addSnapshotListener
                }
                snapshot?.let { trySend(it.toObjects(ChatMessage::class.java)) }
            }

        awaitClose { listener.remove() }
    }

    suspend fun createConversation(userId: String): Result<String> {
        return try {
            val ref = firestore.collection("chatHistory")
                .document(userId)
                .collection("conversations")
                .document()

            ref.set(Conversation(id = ref.id, userId = userId)).await()
            Result.success(ref.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLatestConversation(userId: String): Result<Conversation?> {
        return try {
            val snapshot = firestore.collection("chatHistory")
                .document(userId)
                .collection("conversations")
                .orderBy("lastUpdated", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()
            val conversation = snapshot.toObjects(Conversation::class.java).firstOrNull()
            Result.success(conversation)
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

    private suspend fun parseRecommendations(response: String): Pair<String, List<Recommendation>> {
        val recommendations = mutableListOf<Recommendation>()
        var cleanContent = response

        try {
            val startTag = "[RECOMMENDATIONS]"
            val endTag = "[/RECOMMENDATIONS]"
            val startIndex = response.indexOf(startTag)
            val endIndex = response.indexOf(endTag)

            if (startIndex != -1 && endIndex != -1) {
                val jsonString = response
                    .substring(startIndex + startTag.length, endIndex)
                    .trim()

                cleanContent = response.substring(0, startIndex).trim()

                val jsonArray = JSONArray(jsonString)

                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val type = if (obj.optString("type").uppercase() == "VIDEO") {
                        RecommendationType.VIDEO
                    } else {
                        RecommendationType.BOOK
                    }

                    var title = obj.optString("title").trim()
                    var url = ""
                    var imageUrl = ""

                    if (type == RecommendationType.VIDEO) {
                        val result = withContext(Dispatchers.IO) {
                            youTubeService.searchVideo(title)
                        }
                        if (result != null) {
                            url = result.first
                            imageUrl = result.second
                        }
                    }

                    val stableId = "rec_" + (title + type.name).hashCode().toString()

                    if (title.isNotBlank()) {
                        recommendations.add(
                            Recommendation(
                                id = stableId,
                                type = type,
                                title = title,
                                description = obj.optString("description"),
                                imageUrl = imageUrl,
                                reason = obj.optString("reason"),
                                relevanceScore = 0.0,
                                url = url
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ChatDataManager", "Failed to parse recommendations", e)
        }

        return Pair(cleanContent, recommendations)
    }

    private suspend fun attachContentUrls(
        recommendations: List<Recommendation>,
        curatedBooks: List<Book>,
        approvedVideos: List<ApprovedVideo>
    ): List<Recommendation> {
        return recommendations.mapNotNull { rec ->
            when (rec.type) {
                RecommendationType.BOOK -> {
                    val matchingBook = curatedBooks.firstOrNull { book ->
                        titlesMatch(book.title, rec.title)
                    }

                    if (matchingBook != null) {
                        val bookUrl = matchingBook.readerUrl.ifBlank { matchingBook.bookUrl }
                        rec.copy(
                            id = matchingBook.id,
                            title = matchingBook.title,
                            description = if (rec.description.isBlank()) matchingBook.description else rec.description,
                            imageUrl = matchingBook.coverUrl,
                            url = bookUrl,
                            isCurated = true
                        )
                    } else {
                        // Not in curated list — search Open Library
                        try {
                            val searchResult = withContext(Dispatchers.IO) {
                                openLibraryService.searchBooks(rec.title, limit = 3)
                            }
                            val found = searchResult.docs.firstOrNull { it.canReadOnline() }
                            if (found != null) {
                                rec.copy(
                                    url = found.getReadUrl() ?: "",
                                    imageUrl = found.getCoverUrl("M") ?: "",
                                    isCurated = false
                                )
                            } else {
                                // Fallback: link to Open Library page
                                val anyResult = searchResult.docs.firstOrNull()
                                if (anyResult != null) {
                                    rec.copy(
                                        url = anyResult.getOpenLibraryUrl(),
                                        imageUrl = anyResult.getCoverUrl("M") ?: "",
                                        isCurated = false
                                    )
                                } else {
                                    null
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("ChatDataManager", "Open Library search failed for: ${rec.title}", e)
                            null
                        }
                    }
                }

                RecommendationType.VIDEO -> {
                    val matchingVideo = approvedVideos.firstOrNull { video ->
                        titlesMatch(video.title, rec.title)
                    }

                    if (matchingVideo != null) {
                        rec.copy(
                            id = matchingVideo.id,
                            title = matchingVideo.title,
                            description = if (rec.description.isBlank()) matchingVideo.description else rec.description,
                            imageUrl = matchingVideo.imageUrl,
                            reason = if (rec.reason.isBlank()) matchingVideo.reason else rec.reason,
                            url = matchingVideo.url,
                            isCurated = true
                        )
                    } else if (rec.url.isNotBlank()) {
                        // Has URL from YouTubeService search but not in approved list
                        rec.copy(isCurated = false)
                    } else {
                        null
                    }
                }
            }
        }
    }

    private suspend fun ensureBookAndVideoMix(
        originalMessage: String,
        recommendations: List<Recommendation>,
        curatedBooks: List<Book>,
        approvedVideos: List<ApprovedVideo>
    ): List<Recommendation> {
        val mutable = recommendations.toMutableList()

        val hasBook = mutable.any { it.type == RecommendationType.BOOK && it.url.isNotBlank() }
        val hasVideo = mutable.any { it.type == RecommendationType.VIDEO && it.url.isNotBlank() }

        if (!hasBook && curatedBooks.isNotEmpty()) {
            val fallbackBook = curatedBooks.first()
            val fallbackUrl = fallbackBook.readerUrl.ifBlank { fallbackBook.bookUrl }

            mutable.add(
                Recommendation(
                    id = fallbackBook.id,
                    type = RecommendationType.BOOK,
                    title = fallbackBook.title,
                    description = fallbackBook.description,
                    imageUrl = fallbackBook.coverUrl,
                    reason = "A nice story to read.",
                    relevanceScore = 0.0,
                    url = fallbackUrl
                )
            )
        }

        if (!hasVideo) {
            // Try YouTube search for a query related to the user's message first
            val searchQuery = "$originalMessage kids safe"
            val youtubeResult = try {
                withContext(Dispatchers.IO) { youTubeService.searchVideo(searchQuery) }
            } catch (_: Exception) { null }

            if (youtubeResult != null) {
                mutable.add(
                    Recommendation(
                        id = "yt_fallback_${youtubeResult.first.hashCode()}",
                        type = RecommendationType.VIDEO,
                        title = "Video for you",
                        description = "A video related to what you asked about.",
                        imageUrl = youtubeResult.second,
                        reason = "Found a safe video matching your interests.",
                        relevanceScore = 0.0,
                        url = youtubeResult.first,
                        isCurated = false
                    )
                )
            } else if (approvedVideos.isNotEmpty()) {
                // Fallback to best-matching approved video
                val fallbackVideo = pickBestFallbackVideo(originalMessage, approvedVideos)
                mutable.add(
                    Recommendation(
                        id = fallbackVideo.id,
                        type = RecommendationType.VIDEO,
                        title = fallbackVideo.title,
                        description = fallbackVideo.description,
                        imageUrl = fallbackVideo.imageUrl,
                        reason = fallbackVideo.reason,
                        relevanceScore = 0.0,
                        url = fallbackVideo.url
                    )
                )
            }
        }

        return mutable
    }

    private fun pickBestFallbackVideo(
        message: String,
        videos: List<ApprovedVideo>
    ): ApprovedVideo {
        val lowerMessage = message.lowercase()
        val messageWords = lowerMessage.split(Regex("\\s+")).filter { it.length > 2 }

        val scored = videos.map { video ->
            var score = 0
            // Tag matching
            score += video.tags.count { tag -> lowerMessage.contains(tag.lowercase()) } * 3
            // Title word matching
            val titleWords = video.title.lowercase().split(Regex("\\s+"))
            score += messageWords.count { word -> titleWords.any { it.contains(word) } } * 2
            // Description matching
            score += messageWords.count { word -> video.description.lowercase().contains(word) }
            video to score
        }

        val best = scored.maxByOrNull { it.second }
        // Only return the best match if it actually matched something
        return if (best != null && best.second > 0) best.first else videos.random()
    }

    private fun titlesMatch(a: String, b: String): Boolean {
        val x = normalizeTitle(a)
        val y = normalizeTitle(b)
        return x == y || x.contains(y) || y.contains(x)
    }

    private fun normalizeTitle(value: String): String {
        return value
            .lowercase()
            .replace(Regex("[^a-z0-9 ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

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
                    titlesMatch(book.title, rec.title)
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
}