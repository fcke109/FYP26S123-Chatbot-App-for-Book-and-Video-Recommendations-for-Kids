package com.kidsrec.chatbot.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp

@Keep
enum class PlanType {
    FREE, PREMIUM, ADMIN
}

@Keep
enum class AccountType {
    PARENT, CHILD
}

@Keep
data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val age: Int = 0,
    val planType: PlanType = PlanType.FREE,
    val accountType: AccountType = AccountType.CHILD,
    val parentId: String? = null,
    val childIds: List<String> = emptyList(),
    val interests: List<String> = emptyList(),
    val readingLevel: String = "Beginner",
    val parentalEmail: String? = null,
    val parentalPin: String? = null,
    val contentFilters: ContentFilters = ContentFilters(),
    val screenTimeConfig: ScreenTimeConfig = ScreenTimeConfig(),
    val contentApprovalRequired: Boolean = false,
    val isGuest: Boolean = false,
    val createdAt: Timestamp = Timestamp.now()
) {
    // No-arg constructor for Firestore
    constructor() : this("")
}

@Keep
data class ContentFilters(
    val maxAgeRating: Int = 13,
    val blockedTopics: List<String> = emptyList(),
    val allowVideos: Boolean = true
) {
    // No-arg constructor for Firestore
    constructor() : this(13, emptyList(), true)
}

@Keep
data class ScreenTimeConfig(
    val dailyLimitMinutes: Int = 30,
    val isEnabled: Boolean = true,
    val warningThresholdMinutes: Int = 25
) {
    constructor() : this(30, true, 25)
}
