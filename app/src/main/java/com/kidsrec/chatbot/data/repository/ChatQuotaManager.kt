package com.kidsrec.chatbot.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.kidsrec.chatbot.data.model.PlanType
import com.kidsrec.chatbot.data.model.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

// Stores the current chat quota information for a user
data class ChatQuotaStatus(
    val planType: PlanType,
    val used: Int,
    val limit: Int,
    val resetAt: Timestamp?
) {
    // Remaining free chats left
    val remaining: Int get() = (limit - used).coerceAtLeast(0)
    // Only FREE users are limited
    val limited: Boolean get() = planType == PlanType.FREE
    // True when user has used all free chats
    val exhausted: Boolean get() = limited && remaining <= 0
}

@Singleton
class ChatQuotaManager @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    companion object {
        // Daily free chat limit
        const val FREE_DAILY_LIMIT = 5
        // 24-hour reset window
        private const val WINDOW_MS = 24L * 60L * 60L * 1000L
    }

    // Firestore document reference for each user's quota
    private fun docRef(userId: String) =
        firestore.collection("chatQuota").document(userId)


    // Checks whether the current quota window expired
    private fun isExpired(windowStart: Timestamp?): Boolean {
        if (windowStart == null) return true
        val elapsed = System.currentTimeMillis() - windowStart.toDate().time
        return elapsed >= WINDOW_MS
    }

    // Calculates when the quota resets
    private fun resetAtFor(windowStart: Timestamp?): Timestamp? {
        if (windowStart == null) return null
        return Timestamp(java.util.Date(windowStart.toDate().time + WINDOW_MS))
    }

    // Gets the user's current quota status
    suspend fun getStatus(user: User): ChatQuotaStatus {
        // Premium users have unlimited chats
        if (user.planType != PlanType.FREE) {
            return ChatQuotaStatus(
                planType = user.planType,
                used = 0,
                limit = Int.MAX_VALUE,
                resetAt = null
            )
        }

        return try {
            val snapshot = docRef(user.id).get().await()
            val windowStart = snapshot.getTimestamp("windowStart")
            val count = (snapshot.getLong("count") ?: 0L).toInt()

            // Reset expired quota
            if (isExpired(windowStart)) {
                // Fallback status if Firestore fails
                ChatQuotaStatus(
                    planType = PlanType.FREE,
                    used = 0,
                    limit = FREE_DAILY_LIMIT,
                    resetAt = null
                )
            } else {
                ChatQuotaStatus(
                    planType = PlanType.FREE,
                    used = count,
                    limit = FREE_DAILY_LIMIT,
                    resetAt = resetAtFor(windowStart)
                )
            }
        } catch (_: Exception) {
            ChatQuotaStatus(
                planType = PlanType.FREE,
                used = 0,
                limit = FREE_DAILY_LIMIT,
                resetAt = null
            )
        }
    }

    // Real-time quota listener
    fun statusFlow(user: User): Flow<ChatQuotaStatus> = callbackFlow {
        // Premium users do not need quota tracking
        if (user.planType != PlanType.FREE) {
            trySend(
                ChatQuotaStatus(
                    planType = user.planType,
                    used = 0,
                    limit = Int.MAX_VALUE,
                    resetAt = null
                )
            )
            awaitClose { }
            return@callbackFlow
        }

        // Listen for Firestore quota changes
        val listener = docRef(user.id).addSnapshotListener { snapshot, _ ->
            val windowStart = snapshot?.getTimestamp("windowStart")
            val count = (snapshot?.getLong("count") ?: 0L).toInt()

            val status = if (isExpired(windowStart)) {
                ChatQuotaStatus(
                    planType = PlanType.FREE,
                    used = 0,
                    limit = FREE_DAILY_LIMIT,
                    resetAt = null
                )
            } else {
                ChatQuotaStatus(
                    planType = PlanType.FREE,
                    used = count,
                    limit = FREE_DAILY_LIMIT,
                    resetAt = resetAtFor(windowStart)
                )
            }
            trySend(status)
        }

        awaitClose { listener.remove() }
    }

    // consumes ine chat usage for Free users and Returns updated qouta status if successful
    suspend fun tryConsume(user: User): Result<ChatQuotaStatus> {
        // Premium users bypass quota system
        if (user.planType != PlanType.FREE) {
            return Result.success(
                ChatQuotaStatus(
                    planType = user.planType,
                    used = 0,
                    limit = Int.MAX_VALUE,
                    resetAt = null
                )
            )
        }

        return try {
            val ref = docRef(user.id)
            val snapshot = ref.get().await()
            val windowStart = snapshot.getTimestamp("windowStart")
            val count = (snapshot.getLong("count") ?: 0L).toInt()

            // Start a new quota window if expired
            val (newWindowStart, newCount) = if (isExpired(windowStart)) {
                Timestamp.now() to 1
            } else {
                val updated = count + 1
                // Stop user if limit exceeded
                if (updated > FREE_DAILY_LIMIT) {
                    return Result.failure(
                        Exception(
                            "You've reached your Free plan limit of $FREE_DAILY_LIMIT chat questions for today. Upgrade to Premium or try again later."
                        )
                    )
                }
                (windowStart ?: Timestamp.now()) to updated
            }

            // Save updated quota usage
            ref.set(
                mapOf(
                    "windowStart" to newWindowStart,
                    "count" to newCount,
                    "updatedAt" to Timestamp.now()
                )
            ).await()

            Result.success(
                ChatQuotaStatus(
                    planType = PlanType.FREE,
                    used = newCount,
                    limit = FREE_DAILY_LIMIT,
                    resetAt = resetAtFor(newWindowStart)
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
