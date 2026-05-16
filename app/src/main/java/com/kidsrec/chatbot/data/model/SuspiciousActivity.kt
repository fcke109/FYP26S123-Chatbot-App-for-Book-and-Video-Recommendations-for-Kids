package com.kidsrec.chatbot.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp

// Security severity levels used for suspicious activity tracking
@Keep
enum class SecuritySeverity {
    LOW, MEDIUM, HIGH, CRITICAL
}

// Types of suspicious activities monitored in the app
@Keep
enum class SuspiciousActivityType {
    MULTIPLE_FAILED_LOGINS,
    UNUSUAL_ACCESS_PATTERN,
    SUSPICIOUS_IP_ACTIVITY,
    ACCOUNT_BRUTE_FORCE,
    UNUSUAL_DEVICE_ACTIVITY
}
// Stores detected suspicious/security-related activities
@Keep
data class SuspiciousActivity(
    val id: String = "",
    val userId: String = "",
    val email: String = "",

    // Activity details
    val type: String = "",
    val description: String = "",
    val details: String = "",
    val ipAddress: String = "",

    // Severity and category of the activity
    val severity: SecuritySeverity = SecuritySeverity.LOW,
    val activityType: SuspiciousActivityType = SuspiciousActivityType.MULTIPLE_FAILED_LOGINS,
    val timestamp: Timestamp = Timestamp.now(),

    // Indicates whether the issue has been resolved
    val resolved: Boolean = false
) {
    // Empty constructor required for Firestore
    constructor() : this("")
}
