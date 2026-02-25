package com.kidsrec.chatbot.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface OpenLibraryService {
    @GET("search.json")
    suspend fun searchBooks(@Query("q") query: String): OpenLibrarySearchResponse
}

data class OpenLibrarySearchResponse(
    val docs: List<OpenLibraryDoc>
)

data class OpenLibraryDoc(
    val title: String,
    val author_name: List<String>? = null,
    val cover_i: Int? = null,
    val key: String,
    val ia: List<String>? = null // This contains the REAL Archive.org IDs
)
