package com.kidsrec.chatbot.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp

@Keep
enum class SecuritySeverity {
    LOW, MEDIUM, HIGH, CRITICAL
}

@Keep
enum class SuspiciousActivityType {
    MULTIPLE_FAILED_LOGINS,
    UNUSUAL_ACCESS_PATTERN,
    SUSPICIOUS_IP_ACTIVITY,
    ACCOUNT_BRUTE_FORCE,
    UNUSUAL_DEVICE_ACTIVITY
}

@Keep
data class SuspiciousActivity(
    val id: String = "",
    val userId: String = "",
    val email: String = "",
    val type: String = "",
    val description: String = "",
    val details: String = "",
    val ipAddress: String = "",
    val severity: SecuritySeverity = SecuritySeverity.LOW,
    val activityType: SuspiciousActivityType = SuspiciousActivityType.MULTIPLE_FAILED_LOGINS,
    val timestamp: Timestamp = Timestamp.now(),
    val resolved: Boolean = false
) {
    constructor() : this("")
}
