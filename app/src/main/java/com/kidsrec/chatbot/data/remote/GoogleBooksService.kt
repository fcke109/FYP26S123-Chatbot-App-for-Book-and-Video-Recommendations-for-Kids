package com.kidsrec.chatbot.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

// Retrofit service for accessing the Google Books API
interface GoogleBooksService {
    // Searches for a book and retrieves preview information
    @GET("volumes")
    suspend fun getBookPreviewUrl(@Query("q") title: String): GoogleBooksResponse
}

// Main API response model
data class GoogleBooksResponse(
    val items: List<GoogleBookItem>?
)

// Represents a single book item returned from Google Books
data class GoogleBookItem(
    val volumeInfo: VolumeInfo
)

// Contains book details such as preview links
data class VolumeInfo(
    val previewLink: String?
)
