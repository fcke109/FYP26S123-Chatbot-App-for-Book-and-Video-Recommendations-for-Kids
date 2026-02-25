package com.kidsrec.chatbot.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.kidsrec.chatbot.data.model.Book
import com.kidsrec.chatbot.data.remote.GoogleBooksService
import com.kidsrec.chatbot.data.remote.OpenAIMessage
import com.kidsrec.chatbot.data.remote.OpenAIRequest
import com.kidsrec.chatbot.data.remote.OpenAIService
import com.kidsrec.chatbot.data.remote.OpenLibraryService
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import javax.inject.Inject
import javax.inject.Singleton

/**
 * BookDataManager: Handles all book-related operations including search, storage, and retrieval.
 */
@Singleton
class BookDataManager @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val openLibraryService: OpenLibraryService,
    private val googleBooksService: GoogleBooksService,
    private val openAIService: OpenAIService 
) {
    suspend fun getBookPreviewUrl(title: String): Result<BookPreview> {
        return try {
            val response = googleBooksService.getBookPreviewUrl(title)
            val previewUrl = response.items?.firstOrNull()?.volumeInfo?.previewLink
            if (previewUrl != null) {
                Result.success(BookPreview(previewUrl.replace("http://", "https://")))
            } else {
                Result.failure(Exception("No preview URL found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCuratedBooks(): Result<List<Book>> {
        return try {
            val snapshot = firestore.collection("gutenberg_books").get().await()
            val books = snapshot.toObjects(Book::class.java)
            Result.success(books)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun curateBooksFromText(rawText: String): Result<Int> {
        return try {
            val prompt = """
                You are a library curator. The user has provided text or a URL: "$rawText"
                Identify the book(s) and return ONLY a JSON array of objects with keys: 
                "title", "author", "description", "ageRange", "coverUrl", "openLibraryUrl", "gutenbergId", "gutenbergHtmlUrl", "readTxtUrl".
                Note: Ensure all URLs use https://.
            """.trimIndent()

            val request = OpenAIRequest(
                messages = listOf(OpenAIMessage(role = "user", content = prompt))
            )
            
            val response = openAIService.createChatCompletion(request)
            val jsonContent = response.choices.firstOrNull()?.message?.content ?: ""
            
            val jsonArrayString = if (jsonContent.contains("[")) {
                jsonContent.substring(jsonContent.indexOf("["), jsonContent.lastIndexOf("]") + 1)
            } else {
                jsonContent
            }

            val jsonArray = JSONArray(jsonArrayString)
            var count = 0
            
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val bookId = if (!obj.isNull("gutenbergId")) obj.getInt("gutenbergId").toString() else firestore.collection("gutenberg_books").document().id
                
                val book = Book(
                    id = bookId,
                    title = obj.optString("title", "Unknown Title"),
                    author = obj.optString("author", "Unknown Author"),
                    description = obj.optString("description", ""),
                    ageRating = obj.optString("ageRange", "Ages 6-12"),
                    coverUrl = obj.optString("coverUrl", null)?.replace("http://", "https://"),
                    openLibraryUrl = obj.optString("openLibraryUrl", null)?.replace("http://", "https://"),
                    gutenbergHtmlUrl = obj.optString("gutenbergHtmlUrl", null)?.replace("http://", "https://"),
                    gutenbergTxtUrl = obj.optString("readTxtUrl", null)?.replace("http://", "https://")
                )
                
                firestore.collection("gutenberg_books").document(book.id).set(book).await()
                count++
            }
            
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class BookPreview(
    val previewUrl: String
)
