package com.kidsrec.chatbot.util

/**
 * Client-side defense-in-depth content filter for AI responses.
 * This is a secondary check — primary filtering happens server-side in Cloud Functions.
 */
object ContentFilter {

    // Exact words that are never appropriate in a kids app response
    private val UNSAFE_WORDS = listOf(
        "fuck", "fucking", "fucker", "fucked",
        "shit", "shitty",
        "bitch", "bitches",
        "asshole",
        "bastard",
        "cunt",
        "dick", "dicks",
        "piss", "pissed",
        "whore", "slut",
        "nigger", "nigga", "faggot",
        "porn", "porno", "pornography",
        "xxx",
        "cocaine", "heroin", "meth",
    )

    // Precompiled word-boundary regexes
    private val UNSAFE_WORD_PATTERNS: List<Regex> = UNSAFE_WORDS.map { word ->
        Regex("\\b${Regex.escape(word)}\\b", RegexOption.IGNORE_CASE)
    }

    // Phrase patterns for dangerous content
    private val UNSAFE_PHRASES = listOf(
        Regex("\\bhow to (kill|murder|stab|shoot|hurt)\\b", RegexOption.IGNORE_CASE),
        Regex("\\b(suicide|self-harm|kill yourself|cut yourself|end your life)\\b", RegexOption.IGNORE_CASE),
        Regex("\\bsexual(ly)?\\b", RegexOption.IGNORE_CASE),
        Regex("\\bnaked\\b", RegexOption.IGNORE_CASE),
        Regex("\\bnude\\b", RegexOption.IGNORE_CASE),
        Regex("\\bexplicit\\b", RegexOption.IGNORE_CASE),
        Regex("\\berotic\\b", RegexOption.IGNORE_CASE),
    )

    private const val SAFE_FALLBACK =
        "I'd love to help you find some great books and videos! " +
        "Could you tell me what topics you're interested in?"

    fun isSafe(text: String): Boolean {
        if (UNSAFE_WORD_PATTERNS.any { it.containsMatchIn(text) }) return false
        if (UNSAFE_PHRASES.any { it.containsMatchIn(text) }) return false
        return true
    }

    fun sanitizeResponse(text: String): String {
        return if (isSafe(text)) text else SAFE_FALLBACK
    }
}
