package com.kidsrec.chatbot.ui.common

enum class AgeUiMode {
    EARLY_CHILD,
    YOUNG_CHILD,
    OLDER_CHILD
}

fun getAgeUiMode(age: Int): AgeUiMode {
    return when {
        age <= 5 -> AgeUiMode.EARLY_CHILD
        age <= 8 -> AgeUiMode.YOUNG_CHILD
        else -> AgeUiMode.OLDER_CHILD
    }
}