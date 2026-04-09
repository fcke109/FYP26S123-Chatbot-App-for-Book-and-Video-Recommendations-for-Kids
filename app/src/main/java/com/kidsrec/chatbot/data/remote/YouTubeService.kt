package com.kidsrec.chatbot.data.remote

import android.util.Log
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Result from a YouTube search via the Cloud Function.
 * @param videoUrl  The YouTube video URL
 * @param thumbnailUrl  The video thumbnail URL
 * @param title  The actual video title from YouTube (not the AI-suggested title)
 */
data class YouTubeSearchResult(
    val videoUrl: String,
    val thumbnailUrl: String,
    val title: String
)

@Singleton
class YouTubeService @Inject constructor(
    private val functions: FirebaseFunctions
) {
    /**
     * Searches YouTube via the searchYouTube Cloud Function which enforces
     * safeSearch=strict and videoEmbeddable=true.
     *
     * @return A [YouTubeSearchResult] with the video URL, thumbnail, and actual YouTube title,
     *         or null if the search failed or returned no results.
     */
    suspend fun searchVideo(query: String): YouTubeSearchResult? {
        return try {
            val result = functions
                .getHttpsCallable("searchYouTube")
                .call(mapOf("query" to query))
                .await()

            val data = result.getData() as? Map<*, *> ?: return null
            val videoUrl = data["videoUrl"] as? String ?: return null
            val thumbnailUrl = data["thumbnailUrl"] as? String ?: return null
            val title = data["title"] as? String ?: ""

            // Validate URLs are legitimate YouTube/Google domains
            val videoHost = android.net.Uri.parse(videoUrl).host?.lowercase()
            if (videoHost == null || !(videoHost.endsWith("youtube.com") || videoHost.endsWith("youtu.be"))) {
                Log.w("YouTubeService", "Invalid video URL domain: $videoHost")
                return null
            }

            YouTubeSearchResult(videoUrl, thumbnailUrl, title)
        } catch (e: Exception) {
            Log.e("YouTubeService", "Failed to search YouTube via Cloud Function", e)
            null
        }
    }
}
