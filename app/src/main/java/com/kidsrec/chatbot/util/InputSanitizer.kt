package com.kidsrec.chatbot.util

/**
 * Input sanitization utilities for the chat system.
 * Prevents prompt injection, inappropriate content, vulgar language,
 * and excessively long messages.
 */
object InputSanitizer {

    private const val MAX_MESSAGE_LENGTH = 500

    // Patterns that could be used for prompt injection
    private val INJECTION_PATTERNS = listOf(
        "ignore previous",
        "ignore above",
        "disregard",
        "forget your instructions",
        "you are now",
        "act as",
        "pretend to be",
        "new instructions",
        "system prompt",
        "override",
        "jailbreak"
    )

    // Exact vulgar words (word boundary enforced, no repeated-letter patterns)
    private val INAPPROPRIATE_WORDS = listOf(
        // Profanity - exact words only
        "fuck", "fucker", "fucking", "fucked", "fck", "fuk", "fuking",
        "shit", "shitty", "shitting",
        "bitch", "bitches",
        "asshole", "assholes",
        "bastard", "bastards",
        "dick", "dicks",  // won't match "dickens" due to word boundary
        "piss", "pissed", "pissing",
        "wtf", "stfu", "lmfao",
        "cunt", "cunts",
        "whore", "slut",
        // Slurs
        "nigger", "nigga", "faggot", "fag", "retard", "retarded",
        // Sexual
        "porn", "porno", "pornography",
        "xxx",
        "boobs", "boobies",
        // Drugs
        "cocaine", "heroin", "marijuana", "meth",
    )

    // Phrase patterns (not single words)
    private val INAPPROPRIATE_PHRASES = listOf(
        Regex("\\bhow to kill\\b", RegexOption.IGNORE_CASE),
        Regex("\\bhow to hurt\\b", RegexOption.IGNORE_CASE),
        Regex("\\bhow to make a bomb\\b", RegexOption.IGNORE_CASE),
        Regex("\\bhow to make a weapon\\b", RegexOption.IGNORE_CASE),
        Regex("\\bkill myself\\b", RegexOption.IGNORE_CASE),
        Regex("\\bcut myself\\b", RegexOption.IGNORE_CASE),
        Regex("\\bself[\\s-]?harm\\b", RegexOption.IGNORE_CASE),
        Regex("\\bcommit suicide\\b", RegexOption.IGNORE_CASE),
        Regex("\\bwant to die\\b", RegexOption.IGNORE_CASE),
    )

    // Precompiled word-boundary regexes for exact word matching
    private val INAPPROPRIATE_WORD_PATTERNS: List<Regex> = INAPPROPRIATE_WORDS.map { word ->
        Regex("\\b${Regex.escape(word)}\\b", RegexOption.IGNORE_CASE)
    }

    // Kid-friendly warning messages
    private val KID_FRIENDLY_WARNINGS = listOf(
        "Oops! Let's use kind words. Try asking about a book or video instead!",
        "Hmm, that doesn't sound very nice. How about we talk about your favorite stories?",
        "Let's keep things friendly! What kind of books or videos do you like?",
        "That's not something I can help with. Want me to find a cool story for you?",
        "Let's use nice words! Ask me about animals, space, adventures, or anything fun!"
    )

    /**
     * Sanitize a user chat message before sending to OpenAI.
     */
    fun sanitizeChatMessage(message: String): String {
        var sanitized = message.trim()

        if (sanitized.length > MAX_MESSAGE_LENGTH) {
            sanitized = sanitized.take(MAX_MESSAGE_LENGTH)
        }

        // Remove HTML/script tags
        sanitized = sanitized.replace(Regex("<[^>]*>"), "")

        // Remove potential code injection characters
        sanitized = sanitized.replace("`", "")
            .replace("\\", "")
            .replace("{", "")
            .replace("}", "")

        return sanitized
    }

    /**
     * Check if a message contains prompt injection patterns.
     */
    fun isSafeMessage(message: String): Boolean {
        val lower = message.lowercase()
        return INJECTION_PATTERNS.none { pattern -> lower.contains(pattern) }
    }

    /**
     * Check if message contains vulgar, inappropriate, or unsafe content.
     * Returns true if the message is clean.
     */
    fun isAppropriateForKids(message: String): Boolean {
        // Check exact word matches
        if (INAPPROPRIATE_WORD_PATTERNS.any { it.containsMatchIn(message) }) return false
        // Check phrase patterns
        if (INAPPROPRIATE_PHRASES.any { it.containsMatchIn(message) }) return false
        return true
    }

    /**
     * Get a random kid-friendly warning message.
     */
    fun getKidFriendlyWarning(): String {
        return KID_FRIENDLY_WARNINGS.random()
    }

    /**
     * Validate that a message is acceptable to send.
     * Returns null if valid, or an error message string if invalid.
     */
    fun validateMessage(message: String): String? {
        if (message.isBlank()) return "Message cannot be empty."
        if (message.length > MAX_MESSAGE_LENGTH) return "Message is too long (max $MAX_MESSAGE_LENGTH characters)."
        if (!isSafeMessage(message)) return "Your message contains content that cannot be processed."
        if (!isAppropriateForKids(message)) return getKidFriendlyWarning()
        return null
    }
}
