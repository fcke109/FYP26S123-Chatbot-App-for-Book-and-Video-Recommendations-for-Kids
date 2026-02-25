package com.kidsrec.chatbot.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Google Books API - FREE, no API key needed for basic search
 * Returns direct links to book previews
 */

data class GoogleBooksResponse(
    val totalItems: Int = 0,
    val items: List<GoogleBookItem>? = null
)

data class GoogleBookItem(
    val id: String = "",
    val volumeInfo: GoogleVolumeInfo = GoogleVolumeInfo(),
    val accessInfo: GoogleAccessInfo = GoogleAccessInfo()
)

data class GoogleVolumeInfo(
    val title: String = "",
    val authors: List<String>? = null,
    val description: String? = null,
    val imageLinks: GoogleImageLinks? = null,
    val previewLink: String? = null,       // Link to Google Books preview
    val infoLink: String? = null           // Link to Google Books info page
)

data class GoogleImageLinks(
    val thumbnail: String? = null,
    val smallThumbnail: String? = null
)

data class GoogleAccessInfo(
    val viewability: String = "",          // "NO_PAGES", "PARTIAL", "ALL_PAGES"
    val embeddable: Boolean = false,
    val webReaderLink: String? = null      // Direct link to embedded reader!
)

interface GoogleBooksService {

    // Search for books - returns direct preview links
    // Free API, no key needed for basic usage
    @GET("volumes")
    suspend fun searchBooks(
        @Query("q") query: String,
        @Query("filter") filter: String? = null,  // No filter = more results
        @Query("maxResults") maxResults: Int = 1,
        @Query("printType") printType: String = "books"
    ): GoogleBooksResponse
}
