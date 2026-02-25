package com.kidsrec.chatbot.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface GoogleBooksService {
    @GET("volumes")
    suspend fun getBookPreviewUrl(@Query("q") title: String): GoogleBooksResponse
}

data class GoogleBooksResponse(
    val items: List<GoogleBookItem>?
)

data class GoogleBookItem(
    val volumeInfo: VolumeInfo
)

data class VolumeInfo(
    val previewLink: String?
)
