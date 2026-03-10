package com.kidsrec.chatbot.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp

@Keep
data class ReadingHistory(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val url: String = "",
    val coverUrl: String = "",
    val isVideo: Boolean = false,
    val openedAt: Timestamp = Timestamp.now()
) {
    constructor() : this("", "", "", "", "", false, Timestamp.now())
}
