package com.kidsrec.chatbot.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GutenbergManager @Inject constructor() {
    private val client = OkHttpClient()

    suspend fun fetchPlainText(url: String): String {
        val safeUrl = url.replace("http://", "https://")
        val req = Request.Builder().url(safeUrl).build()
        return withContext(Dispatchers.IO) {
            try {
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) throw Exception("HTTP ${resp.code}")
                    resp.body?.string().orEmpty()
                }
            } catch (e: Exception) {
                throw e
            }
        }
    }
}
