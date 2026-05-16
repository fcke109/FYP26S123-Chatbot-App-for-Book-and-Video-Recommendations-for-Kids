package com.kidsrec.chatbot.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

// Handles fetching plain text story content from project Gutenberg URLs
@Singleton
class GutenbergManager @Inject constructor() {

    // HTTP client used for network requests
    private val client = OkHttpClient()

   // Downloads and returns plain text content
    suspend fun fetchPlainText(url: String): String {
       // Convert HTTP links to HTTPS for safer requests
       val safeUrl = url.replace("http://", "https://")
       // Create network request
       val req = Request.Builder().url(safeUrl).build()
        return withContext(Dispatchers.IO) {
            try {
                // Execute request in background thread
                client.newCall(req).execute().use { resp ->

                    // Throw error if request fails
                    if (!resp.isSuccessful) throw Exception("HTTP ${resp.code}")

                    // Return response body as plain text
                    resp.body?.string().orEmpty()
                }
            } catch (e: Exception) {
                // Rethrow exception for handling outside
                throw e
            }
        }
    }
}
