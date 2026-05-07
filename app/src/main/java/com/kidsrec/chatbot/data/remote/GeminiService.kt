package com.kidsrec.chatbot.data.remote

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import javax.inject.Inject
import javax.inject.Singleton

data class ChatTurn(
    val role: String,
    val content: String
)

@Singleton
class GeminiService @Inject constructor() {

    private val model by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI())
            .generativeModel(
                modelName = "gemini-2.5-flash",
                generationConfig = generationConfig {
                    temperature = 0.7f
                    maxOutputTokens = 500
                }
            )
    }

    suspend fun chat(
        systemPrompt: String,
        conversationHistory: List<ChatTurn>,
        userMessage: String
    ): String {
        return try {
            val history = conversationHistory.map { msg ->
                content(role = if (msg.role == "user") "user" else "model") {
                    text(msg.content)
                }
            }

            val chat = model.startChat(history)

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
