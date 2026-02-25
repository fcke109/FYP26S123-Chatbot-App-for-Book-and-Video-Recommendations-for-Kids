package com.kidsrec.chatbot.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface OpenLibraryService {
    @GET("search.json")
    suspend fun searchBooks(
        @Query("q") query: String,
        @Query("limit") limit: Int? = null
    ): OpenLibrarySearchResponse
}

data class OpenLibrarySearchResponse(
    val docs: List<OpenLibraryBook>
)

data class OpenLibraryBook(
    val title: String,
    val author_name: List<String>? = null,
    val cover_i: Int? = null,
    val key: String,
    val ia: List<String>? = null
) {
    fun canReadOnline(): Boolean = !ia.isNullOrEmpty()

    fun getReadUrl(): String? {
        val archiveId = ia?.firstOrNull() ?: return null
        return "https://archive.org/details/$archiveId"
    }

    fun getAuthorString(): String = author_name?.joinToString(", ") ?: "Unknown Author"

    fun getCoverUrl(size: String = "M"): String? {
        return cover_i?.let { "https://covers.openlibrary.org/b/id/$it-$size.jpg" }
    }

    fun getOpenLibraryUrl(): String = "https://openlibrary.org$key"
}
