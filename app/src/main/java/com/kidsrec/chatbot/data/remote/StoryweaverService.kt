package com.kidsrec.chatbot.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

// Retrofit service for accessing the Storyweaver API
interface StoryweaverService {
    // Searches children's stories from Storyweaver
    @GET("api/v1/stories")
    suspend fun searchStories(
        @Query("search_query") query: String, // Restored to search_query
        @Query("language") language: String = "English",
        @Query("per_page") limit: Int = 20
    ): StoryweaverResponse
}

// Main API response model
data class StoryweaverResponse(
    val stories: List<SWStory>
)

data class SWStory(
    val id: Int,
    val title: String,
    val authors: List<SWAuthor>,
    val synopsis: String?,
    val slug: String,
    val image_url: String?,
    val reading_level: String?
)

data class SWAuthor(
    val name: String
)
