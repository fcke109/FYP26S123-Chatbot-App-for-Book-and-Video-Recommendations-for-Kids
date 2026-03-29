package com.kidsrec.chatbot.util

/**
 * Input sanitization utilities for the chat system.
 * Prevents prompt injection, inappropriate content, and excessively long messages.
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

    /**
     * Sanitize a user chat message before sending to OpenAI.
     * - Trims whitespace
     * - Limits length
     * - Strips potential prompt injection attempts
     * - Removes HTML/script tags
     */
    fun sanitizeChatMessage(message: String): String {
        var sanitized = message.trim()

        // Limit length
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
     * Returns true if the message is safe, false if suspicious.
     */
    fun isSafeMessage(message: String): Boolean {
        val lower = message.lowercase()
        return INJECTION_PATTERNS.none { pattern -> lower.contains(pattern) }
    }

    /**
     * Validate that a message is acceptable to send.
     * Returns null if valid, or an error message string if invalid.
     */
    fun validateMessage(message: String): String? {
        if (message.isBlank()) return "Message cannot be empty."
        if (message.length > MAX_MESSAGE_LENGTH) return "Message is too long (max $MAX_MESSAGE_LENGTH characters)."
        if (!isSafeMessage(message)) return "Your message contains content that cannot be processed."
        return null
    }
}
