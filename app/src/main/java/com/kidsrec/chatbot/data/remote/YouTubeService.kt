package com.kidsrec.chatbot.data.remote

import android.util.Log
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YouTubeService @Inject constructor(
    private val functions: FirebaseFunctions
) {
    suspend fun searchVideo(query: String): Pair<String, String>? {
        return try {
            val result = functions
                .getHttpsCallable("searchYouTube")
                .call(mapOf("query" to query))
                .await()

            val data = result.getData() as? Map<*, *> ?: return null
            val videoUrl = data["videoUrl"] as? String ?: return null
            val thumbnailUrl = data["thumbnailUrl"] as? String ?: return null

            // Validate URLs are legitimate YouTube/Google domains
            val videoHost = android.net.Uri.parse(videoUrl).host?.lowercase()
            if (videoHost == null || !(videoHost.endsWith("youtube.com") || videoHost.endsWith("youtu.be"))) {
                Log.w("YouTubeService", "Invalid video URL domain: $videoHost")
                return null
            }

            Pair(videoUrl, thumbnailUrl)
        } catch (e: Exception) {
            Log.e("YouTubeService", "Failed to search YouTube via Cloud Function", e)
            null
        }
    }
}
