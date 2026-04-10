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
1. For casual messages like greetings ("hi", "hello", "hey", "what's up", "how are you"), jokes, thank-yous, or general chitchat that do NOT ask about a topic, book, or video:
   - Reply with a short, friendly message ONLY. Do NOT include a [RECOMMENDATIONS] block.
   - Example: "hi" → "Hey there! I'm Little Dino, your story buddy! What kind of books or videos are you in the mood for today?"
2. For ANY message that mentions a topic, subject, character, or interest (e.g. "alice in wonderland", "sharks", "dinosaurs", "I like space"), you MUST include the [RECOMMENDATIONS] block. Even if asking a question, if a topic is present, include recommendations.
3. When recommending, ALWAYS recommend content that DIRECTLY matches what the child asked about. Relevance is the #1 priority.
4. For BOOKS: If a curated book matches the child's topic, use that exact title. Otherwise, suggest a real, well-known children's book about their specific topic (e.g. if they say "superman", recommend a Superman book, NOT a random unrelated book).
5. For VIDEOS: ONLY use an approved video if it DIRECTLY matches what the child asked about (e.g. child asks about "sharks" → "Baby Shark Dance" is a match). If NO approved video matches their topic, recommend a video title that closely describes what they want (e.g. child asks about "superman" → use "Superman Cartoon for Kids" NOT "Dinosaurs for Kids").
6. NEVER substitute an unrelated approved video just because it exists. If the child asks about "superman" and there is no superman video in the approved list, recommend a searchable superman video title instead.
7. For any recommendation, provide a reason why it is fun for the child.
8. When recommending, include a mix of BOTH at least 1 BOOK and at least 1 VIDEO.
9. Keep the response friendly and short for children.

Response format when recommendations are needed:
1. Friendly message (1-2 sentences).
2. End with this EXACT block:

[RECOMMENDATIONS]
[
  {"type":"BOOK","title":"Exact Book Title","description":"1 short sentence","reason":"Why it is fun"},
  {"type":"VIDEO","title":"Video Title","description":"1 short sentence","reason":"Why it is fun"}
]
[/RECOMMENDATIONS]

Response format for casual/greeting messages:
- Just a friendly message. No [RECOMMENDATIONS] block.

RULES FOR JSON:
- type must be BOOK or VIDEO
- for BOOK, if a curated book directly matches the child's interest, use that exact title. Otherwise, use a real, well-known children's book title about what they asked for.
- for VIDEO, if an approved video DIRECTLY matches the child's topic, use that exact title. Otherwise, use a clear, specific, searchable title about the child's topic (e.g. "Superman Cartoon for Kids", "Peppa Pig Full Episode", "Pokemon Adventures for Kids"). The system will search YouTube for this title.
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

            // Detect casual messages that don't need recommendations
            val casualPatterns = listOf(
                "^(hi|hey|hello|hiya|yo|sup|howdy|hola)([!?.\\s]|$)",
                "^(how are you|what's up|whats up|good morning|good afternoon|good evening|good night)",
                "^(thanks|thank you|thx|ty|ok|okay|cool|nice|great|awesome|bye|goodbye|see ya)",
                "^(who are you|what are you|what can you do|help)"
            )
            val isCasualMessage = casualPatterns.any {
                Regex(it, RegexOption.IGNORE_CASE).containsMatchIn(sanitizedMessage.trim())
            }

            // Run recommendation pipeline: always for topic messages, skip for casual greetings
            val recommendations = if (parsedRecs.isNotEmpty()) {
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

                scoreWithANN(
                    recommendations = ensuredMix,
                    curatedBooks = curatedBooks,
                    userId = userId
                )
            } else if (!isCasualMessage) {
                // AI didn't include [RECOMMENDATIONS] block but this is a topic query — force fallback
                ensureBookAndVideoMix(
                    originalMessage = sanitizedMessage,
                    recommendations = emptyList(),
                    curatedBooks = curatedBooks,
                    approvedVideos = approvedVideos
                )
            } else {
                emptyList()
            }

            val botMessage = ChatMessage(
                id = firestore.collection("chatHistory")
                    .document(userId)
                    .collection("conversations")
                    .document(conversationId)
                    .collection("messages")
                    .document().id,
                role = MessageRole.ASSISTANT,
                content = cleanContent.ifBlank { botResponse },
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
                        // Try searching YouTube with the AI-recommended title
                        val result = withContext(Dispatchers.IO) {
                            youTubeService.searchVideo(title)
                        }
                        if (result != null) {
                            url = result.videoUrl
                            imageUrl = result.thumbnailUrl
                            // Use the real YouTube title so it matches the actual video
                            if (result.title.isNotBlank()) title = result.title
                        } else {
                            // Retry with "for kids" appended for better results
                            val retryResult = withContext(Dispatchers.IO) {
                                youTubeService.searchVideo("$title for kids")
                            }
                            if (retryResult != null) {
                                url = retryResult.videoUrl
                                imageUrl = retryResult.thumbnailUrl
                                if (retryResult.title.isNotBlank()) title = retryResult.title
                            }
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
                                url = url,
                                isCurated = false // YouTube search results are not pre-reviewed
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

    /**
     * Extracts the main topic keywords from a user message for better YouTube search.
     * Strips common filler words to get the actual topic.
     */
    private fun extractSearchTopic(message: String): String {
        val fillerWords = setOf(
            "i", "me", "my", "like", "love", "want", "show", "find", "get",
            "watch", "see", "please", "can", "you", "the", "a", "an", "some",
            "about", "tell", "more", "really", "very", "so", "would", "could",
            "recommend", "suggest", "something", "anything", "videos", "video",
            "books", "book", "stories", "story", "to", "of", "for", "and",
            "is", "are", "was", "were", "do", "does", "did", "have", "has",
            "know", "think", "looking", "interested", "in", "on", "with"
        )
        val words = message.lowercase()
            .replace(Regex("[^a-z0-9 ]"), " ")
            .split(Regex("\\s+"))
            .filter { it.length > 1 && it !in fillerWords }

        return words.joinToString(" ").ifBlank { message }
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
            val topic = extractSearchTopic(originalMessage).lowercase()
            val fallbackBook = curatedBooks.firstOrNull { book ->
                book.title.lowercase().contains(topic) ||
                book.description.lowercase().contains(topic) ||
                book.author.lowercase().contains(topic) ||
                topic.split(" ").any { word ->
                    word.length > 2 && (book.title.lowercase().contains(word) || book.description.lowercase().contains(word))
                }
            } ?: curatedBooks.first()
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
            // Extract the actual topic from the user's message for better search
            val topic = extractSearchTopic(originalMessage)
            val searchQuery = "$topic for kids"
            Log.d("ChatDataManager", "Video fallback: searching YouTube for '$searchQuery' (from message: '$originalMessage')")

            val youtubeResult = try {
                withContext(Dispatchers.IO) { youTubeService.searchVideo(searchQuery) }
            } catch (_: Exception) { null }

            if (youtubeResult != null) {
                val displayTitle = youtubeResult.title.ifBlank { "$topic video" }
                mutable.add(
                    Recommendation(
                        id = "yt_fallback_${youtubeResult.videoUrl.hashCode()}",
                        type = RecommendationType.VIDEO,
                        title = displayTitle,
                        description = "A fun video about $topic for kids.",
                        imageUrl = youtubeResult.thumbnailUrl,
                        reason = "Found a safe video about $topic.",
                        relevanceScore = 0.0,
                        url = youtubeResult.videoUrl,
                        isCurated = false
                    )
                )
            } else {
                // YouTube search failed — try a second time with simpler query
                val simpleResult = try {
                    withContext(Dispatchers.IO) { youTubeService.searchVideo("$topic kids") }
                } catch (_: Exception) { null }

                if (simpleResult != null) {
                    val displayTitle = simpleResult.title.ifBlank { "$topic video" }
                    mutable.add(
                        Recommendation(
                            id = "yt_fallback_${simpleResult.videoUrl.hashCode()}",
                            type = RecommendationType.VIDEO,
                            title = displayTitle,
                            description = "A fun video about $topic for kids.",
                            imageUrl = simpleResult.thumbnailUrl,
                            reason = "Found a safe video about $topic.",
                            relevanceScore = 0.0,
                            url = simpleResult.videoUrl,
                            isCurated = false
                        )
                    )
                } else if (approvedVideos.isNotEmpty()) {
                    // Last resort: pick from approved list but ONLY if there's a real match
                    val fallbackVideo = pickBestFallbackVideo(originalMessage, approvedVideos)
                    if (fallbackVideo != null) {
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
                    // If no approved video matches either, we simply don't add a video
                    // rather than showing an irrelevant one
                }
            }
        }

        return mutable
    }

    /**
     * Picks the best matching fallback video from the approved list.
     * Returns null if no video has a meaningful match (score > 0),
     * preventing random irrelevant videos from being shown.
     */
    private fun pickBestFallbackVideo(
        message: String,
        videos: List<ApprovedVideo>
    ): ApprovedVideo? {
        val lowerMessage = message.lowercase()
        val messageWords = lowerMessage.split(Regex("\\s+")).filter { it.length > 2 }

        val scored = videos.map { video ->
            var score = 0
            // Tag matching (weight: 3x)
            score += video.tags.count { tag -> lowerMessage.contains(tag.lowercase()) } * 3
            // Title word matching (weight: 2x)
            val titleWords = video.title.lowercase().split(Regex("\\s+"))
            score += messageWords.count { word -> titleWords.any { it.contains(word) } } * 2
            // Description matching (weight: 1x)
            score += messageWords.count { word -> video.description.lowercase().contains(word) }
            video to score
        }

        val best = scored.maxByOrNull { it.second }
        // Only return a video if it actually matched something — never return random
        return if (best != null && best.second > 0) best.first else null
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

    /**
     * Generate and send initial recommendations for a new user based on their interests.
     * Called once when a child account first opens the chat.
     */
    suspend fun sendInitialRecommendations(
        userId: String,
        conversationId: String
    ): Result<ChatMessage> {
        return try {
            val userDoc = firestore.collection("users").document(userId).get().await()
            val user = userDoc.toObject(User::class.java)
                ?: return Result.failure(Exception("User not found"))

            if (user.interests.isEmpty()) {
                return Result.failure(Exception("No interests set"))
            }

            val curatedBooks = bookDataManager.getCuratedBooks().getOrDefault(emptyList())
            val favorites = favoritesManager.getFavorites(userId)

            val topPicks = recommendationEngine.getTopRecommendations(
                curatedBooks = curatedBooks,
                user = user,
                favorites = favorites,
                limit = 4
            )

            if (topPicks.isEmpty()) {
                return Result.failure(Exception("No recommendations available"))
            }

            val interestsList = user.interests.take(3).joinToString(", ")
            val welcomeContent = "Welcome! Since you like $interestsList, here are some books and videos I picked just for you!"

            val botMessage = ChatMessage(
                id = firestore.collection("chatHistory")
                    .document(userId)
                    .collection("conversations")
                    .document(conversationId)
                    .collection("messages")
                    .document().id,
                role = MessageRole.ASSISTANT,
                content = welcomeContent,
                timestamp = Timestamp.now(),
                recommendations = topPicks
            )

            firestore.collection("chatHistory")
                .document(userId)
                .collection("conversations")
                .document(conversationId)
                .collection("messages")
                .document(botMessage.id)
                .set(botMessage)
                .await()

            firestore.collection("chatHistory")
                .document(userId)
                .collection("conversations")
                .document(conversationId)
                .update(
                    mapOf(
                        "lastUpdated" to Timestamp.now(),
                        "preview" to welcomeContent.take(80)
                    )
                )
                .await()

            Result.success(botMessage)
        } catch (e: Exception) {
            Log.e("ChatDataManager", "sendInitialRecommendations failed", e)
            Result.failure(e)
        }
    }
}