package com.kidsrec.chatbot.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp

// Status options for parent content approval requests
@Keep
enum class ApprovalStatus {
    PENDING, APPROVED, REJECTED
}
// Stores approval requests sent from child accounts to parents
@Keep
data class ContentApproval(
    val id: String = "",
    val childId: String = "",
    val parentId: String = "",

    // Requested content information
    val contentId: String = "",
    val contentTitle: String = "",
    val contentType: String = "",  // "book" or "video"
    val contentUrl: String = "",
    val contentImageUrl: String = "",

    // Current approval status
    val status: ApprovalStatus = ApprovalStatus.PENDING,
    val requestedAt: Timestamp = Timestamp.now(),
    val decidedAt: Timestamp? = null
) {

    // Empty constructor required for Firestore
    constructor() : this("")
}
