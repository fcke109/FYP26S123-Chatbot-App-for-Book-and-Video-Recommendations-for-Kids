package com.kidsrec.chatbot.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

// Retrofit service for accessing the Open Library API
interface OpenLibraryService {

    // Searches books from Open Library
    @GET("search.json")
    suspend fun searchBooks(
        @Query("q") query: String,

        // Optional result limit
        @Query("limit") limit: Int? = null
    ): OpenLibrarySearchResponse
}

// Main API response model
data class OpenLibrarySearchResponse(
    val docs: List<OpenLibraryBook>
)

data class OpenLibraryBook(
    val title: String,
    val author_name: List<String>? = null,
    val cover_i: Int? = null,
    val key: String,
    val ia: List<String>? = null,
    val subject: List<String>? = null
) {
    // Checks if the book can be read online through Internet Archive
    fun canReadOnline(): Boolean = !ia.isNullOrEmpty()

    // Generates Internet Archive reading URL
    fun getReadUrl(): String? {
        val archiveId = ia?.firstOrNull() ?: return null
        return "https://archive.org/details/$archiveId"
    }

    // Converts author list into a readable string
    fun getAuthorString(): String = author_name?.joinToString(", ") ?: "Unknown Author"

    // Generates book cover image URL
    fun getCoverUrl(size: String = "M"): String? {
        return cover_i?.let { "https://covers.openlibrary.org/b/id/$it-$size.jpg" }
    }

    // Generates Open Library page URL
    fun getOpenLibraryUrl(): String = "https://openlibrary.org$key"
}
