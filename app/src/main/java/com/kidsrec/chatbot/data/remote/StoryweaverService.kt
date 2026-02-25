package com.kidsrec.chatbot.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface StoryweaverService {
    @GET("api/v1/stories")
    suspend fun searchStories(
        @Query("search_query") query: String,
        @Query("language") language: String = "English",
        @Query("per_page") limit: Int = 20
    ): StoryweaverResponse
}

data class StoryweaverResponse(
    val stories: List<SWStory>
)

data class StoryweaverStoryResponse(
    val story: SWStory
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
