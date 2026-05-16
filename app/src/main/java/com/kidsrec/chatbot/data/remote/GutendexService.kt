package com.kidsrec.chatbot.data.remote

import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

// Retrofit service for accessing the Gutendex (Project Gutenberg) API
interface GutendexService {

    // Retrieves children's books in English
    @GET("books?topic=children&languages=en")
    suspend fun getChildrenBooks(): GutendexResponse

    // Searches children's books using a keyword
    @GET("books?topic=children&languages=en")
    suspend fun searchBooks(
        @Query("search") query: String
    ): GutendexResponse

    // Loads the next page of results using pagination URL
    @GET
    suspend fun getNextPage(@Url url: String): GutendexResponse
}

// Main API response model
data class GutendexResponse(
    val count: Int,
    val next: String?,
    val previous: String?,
    val results: List<GutendexBook>
)

// Represents a single book returned from Gutendex
data class GutendexBook(
    val id: Int,
    val title: String,
    val authors: List<GutendexAuthor>,
    val subjects: List<String>,
    val formats: Map<String, String>
)

// Represents an author of a Gutendex book
data class GutendexAuthor(
    val name: String
)
