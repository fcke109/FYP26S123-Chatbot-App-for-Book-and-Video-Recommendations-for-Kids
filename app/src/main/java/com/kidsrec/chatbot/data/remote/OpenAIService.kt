package com.kidsrec.chatbot.data.remote

import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

data class OpenAIMessage(
    val role: String,
    val content: String
)

data class OpenAIRequest(
    val model: String = "gpt-3.5-turbo",
    val messages: List<OpenAIMessage>,
    val temperature: Double = 0.7,
    val max_tokens: Int = 500
)

data class OpenAIChoice(
    val message: OpenAIMessage,
    val finish_reason: String
)

data class OpenAIResponse(
    val choices: List<OpenAIChoice>
)

interface OpenAIService {
    @Headers("Content-Type: application/json")
    @POST("chat/completions")
    suspend fun createChatCompletion(@Body request: OpenAIRequest): OpenAIResponse
}
