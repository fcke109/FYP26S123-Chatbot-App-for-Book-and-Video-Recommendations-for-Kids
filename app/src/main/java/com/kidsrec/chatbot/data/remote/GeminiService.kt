package com.kidsrec.chatbot.data.remote

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.ai.type.thinkingConfig
import javax.inject.Inject
import javax.inject.Singleton


// Represents a single chat message used in conversation history
data class ChatTurn(
    val role: String,
    val content: String
)

@Singleton
class GeminiService @Inject constructor() {

    // Sends chatbot requests to the Gemini AI model
    suspend fun chat(
        systemPrompt: String,
        conversationHistory: List<ChatTurn>,
        userMessage: String
    ): String {
        return try {


            // Configure Gemini model settings
            val model = Firebase.ai(backend = GenerativeBackend.googleAI())
                .generativeModel(
                    modelName = "gemini-2.5-flash-lite",
                    systemInstruction = content { text(systemPrompt) },

                    // AI response configuration
                    generationConfig = generationConfig {
                        temperature = 0.5f
                        maxOutputTokens = 8192

                        // Disable reasoning/thinking tokens to avoid token limit issues
                        thinkingConfig = thinkingConfig {
                            thinkingBudget = 0
                        }
                    }
                )

            // Convert previous conversation into Gemini chat history format
            val history = conversationHistory.map { msg ->
                content(role = if (msg.role == "user") "user" else "model") {
                    text(msg.content)
                }
            }

            // Start chat session with conversation history
            val chat = model.startChat(history)
            val response = chat.sendMessage(userMessage)
            response.text ?: "Let's find some fun stories and videos!"
        } catch (e: Exception) {

            // Log Gemini API errors for debugging
            Log.e("GeminiService", "Gemini API call failed: ${e.message}", e)
            throw e
        }
    }
}
