package com.kidsrec.chatbot.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp

// plan types
@Keep
enum class PlanType {
    FREE, PREMIUM, ADMIN
}

// parent or child account
@Keep
enum class AccountType {
    PARENT, CHILD
}

// account status
@Keep
enum class UserStatus {
    ACTIVE, SUSPENDED, BANNED
}

@Keep
data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val age: Int = 0,

    val planType: PlanType = PlanType.FREE,
    val accountType: AccountType = AccountType.CHILD,
    val status: UserStatus = UserStatus.ACTIVE,

    // parent-child relation
    val parentId: String? = null,
    val childIds: List<String> = emptyList(),

    // preferences
    val interests: List<String> = emptyList(),
    val readingLevel: String = "Beginner",

    // parent control
    val parentalEmail: String? = null,
    val parentalPin: String? = null,

    val contentFilters: ContentFilters = ContentFilters(),

    // screen time settings
    val screenTimeConfig: ScreenTimeConfig = ScreenTimeConfig(),

    // 🔥 NEW: track usage for time limit
    val todayUsageMinutes: Int = 0,        // how many mins used today
    val lastUsageDate: String = "",        // reset daily (yyyy-MM-dd)

    val contentApprovalRequired: Boolean = false,
    val isGuest: Boolean = false,

    val createdAt: Timestamp = Timestamp.now()
) {
    // required for firestore
    constructor() : this("")

    // helpers
    val isFreePlan: Boolean
        get() = planType == PlanType.FREE

    val isPremiumPlan: Boolean
        get() = planType == PlanType.PREMIUM

    val isAdminPlan: Boolean
        get() = planType == PlanType.ADMIN
}

// content filtering
@Keep
data class ContentFilters(
    val maxAgeRating: Int = 13,
    val blockedTopics: List<String> = emptyList(),
    val allowVideos: Boolean = true
) {
    constructor() : this(13, emptyList(), true)
}

// screen time config (parent sets this)
@Keep
data class ScreenTimeConfig(
    var dailyLimitMinutes: Int = 30,
    var enabled: Boolean = true,
    var warningThresholdMinutes: Int = 25
) {
    constructor() : this(30, true, 25)
}