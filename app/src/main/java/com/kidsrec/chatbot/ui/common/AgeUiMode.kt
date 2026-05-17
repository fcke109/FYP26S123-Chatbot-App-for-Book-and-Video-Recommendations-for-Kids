package com.kidsrec.chatbot.ui.common

// Different UI modes based on child's age group
enum class AgeUiMode {

    EARLY_CHILD,    // UI for ages 3–5
    YOUNG_CHILD, // UI for ages 6–8
    OLDER_CHILD  // UI for ages 9+
}

// Returns the correct UI mode based on user age
fun getAgeUiMode(age: Int): AgeUiMode {
    return when {
        age <= 5 -> AgeUiMode.EARLY_CHILD // Younger children
        age <= 8 -> AgeUiMode.YOUNG_CHILD // Middle age group
        else -> AgeUiMode.OLDER_CHILD  // Older children
    }
}