package com.kidsrec.chatbot.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp

@Keep
enum class ApprovalStatus {
    PENDING, APPROVED, REJECTED
}

@Keep
data class ContentApproval(
    val id: String = "",
    val childId: String = "",
    val parentId: String = "",
    val contentId: String = "",
    val contentTitle: String = "",
    val contentType: String = "",  // "book" or "video"
    val contentUrl: String = "",
    val contentImageUrl: String = "",
    val status: ApprovalStatus = ApprovalStatus.PENDING,
    val requestedAt: Timestamp = Timestamp.now(),
    val decidedAt: Timestamp? = null
) {
    constructor() : this("")
}
