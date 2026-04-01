package com.kidsrec.chatbot.data.remote

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import com.kidsrec.chatbot.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiService @Inject constructor() {

    private val model by lazy {
        GenerativeModel(
            modelName = "gemini-2.0-flash",
            apiKey = BuildConfig.GEMINI_API_KEY,
            generationConfig = generationConfig {
                temperature = 0.7f
                maxOutputTokens = 500
            }
        )
    }

    suspend fun chat(systemPrompt: String, conversationHistory: List<OpenAIMessage>, userMessage: String): String {
        return try {
            val history = conversationHistory.map { msg ->
                content(role = if (msg.role == "user") "user" else "model") {
                    text(msg.content)
                }
            }

            val chat = model.startChat(history)

            // Prepend system prompt to user message for context
            val fullMessage = if (conversationHistory.isEmpty()) {
                "$systemPrompt\n\nUser message: $userMessage"
            } else {
                userMessage
            }

            val response = chat.sendMessage(fullMessage)
            response.text ?: "Let's find some fun stories and videos!"
        } catch (e: Exception) {
            Log.e("GeminiService", "Gemini API call failed: ${e.message}", e)
            throw e
        }
    }
}
