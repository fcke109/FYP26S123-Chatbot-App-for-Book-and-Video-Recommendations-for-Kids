package com.kidsrec.chatbot.data.remote

import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

interface GutendexService {
    @GET("books?topic=children&languages=en")
    suspend fun getChildrenBooks(): GutendexResponse

    @GET("books?topic=children&languages=en")
    suspend fun searchBooks(
        @Query("search") query: String
    ): GutendexResponse

    @GET
    suspend fun getNextPage(@Url url: String): GutendexResponse
}

data class GutendexResponse(
    val count: Int,
    val next: String?,
    val previous: String?,
    val results: List<GutendexBook>
)

data class GutendexBook(
    val id: Int,
    val title: String,
    val authors: List<GutendexAuthor>,
    val subjects: List<String>,
    val formats: Map<String, String>
)

data class GutendexAuthor(
    val name: String
)
