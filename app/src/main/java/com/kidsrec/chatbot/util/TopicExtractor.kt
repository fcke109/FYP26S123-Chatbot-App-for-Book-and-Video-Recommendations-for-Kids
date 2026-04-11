package com.kidsrec.chatbot.util

object TopicExtractor {

    fun extractTopic(message: String): String {
        val fillerWords = setOf(
            "i", "me", "my", "like", "love", "want", "show", "find", "get",
            "watch", "see", "please", "can", "you", "the", "a", "an", "some",
            "about", "tell", "more", "really", "very", "so", "would", "could",
            "recommend", "suggest", "something", "anything", "videos", "video",
            "books", "book", "stories", "story", "to", "of", "for", "and",
            "is", "are", "was", "were", "do", "does", "did", "have", "has",
            "know", "think", "looking", "interested", "in", "on", "with"
        )

        val words = message.lowercase()
            .replace(Regex("[^a-z0-9 ]"), " ")
            .split(Regex("\\s+"))
            .filter { it.length > 1 && it !in fillerWords }

        return words.take(3).joinToString(" ").ifBlank { "general" }
    }
}