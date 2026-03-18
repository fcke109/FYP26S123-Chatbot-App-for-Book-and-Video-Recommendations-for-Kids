package com.kidsrec.chatbot.data.remote

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

object YouTubeService {

    private const val API_KEY = "AIzaSyA7BQfi3VQRQGC3gnzWLG3mkasat1d5dA4"

    fun searchVideo(query: String): Pair<String, String>? {

        return try {

            val url =
                "https://www.googleapis.com/youtube/v3/search" +
                        "?part=snippet" +
                        "&maxResults=1" +
                        "&type=video" +
                        "&videoEmbeddable=true" +
                        "&safeSearch=strict" +
                        "&q=${query.replace(" ", "%20")}" +
                        "&key=$API_KEY"

            val client = OkHttpClient()
            val request = Request.Builder().url(url).build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return null

            val json = JSONObject(body)
            val items = json.getJSONArray("items")

            if (items.length() == 0) return null

            val item = items.getJSONObject(0)

            val videoId =
                item.getJSONObject("id").getString("videoId")

            val title =
                item.getJSONObject("snippet").getString("title")

            val videoUrl = "https://www.youtube.com/watch?v=$videoId"
            val thumbnail = "https://img.youtube.com/vi/$videoId/hqdefault.jpg"

            Pair(videoUrl, thumbnail)

        } catch (e: Exception) {
            null
        }
    }
}