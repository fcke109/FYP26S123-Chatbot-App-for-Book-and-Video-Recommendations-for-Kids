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

data class ChatQuotaStatus(
    val planType: PlanType,
    val used: Int,
    val limit: Int,
    val resetAt: Timestamp?
) {
    val remaining: Int get() = (limit - used).coerceAtLeast(0)
    val limited: Boolean get() = planType == PlanType.FREE
    val exhausted: Boolean get() = limited && remaining <= 0
}

@Singleton
class ChatQuotaManager @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    companion object {
        const val FREE_DAILY_LIMIT = 5
        private const val WINDOW_MS = 24L * 60L * 60L * 1000L
    }

    private fun docRef(userId: String) =
        firestore.collection("chatQuota").document(userId)

    private fun isExpired(windowStart: Timestamp?): Boolean {
        if (windowStart == null) return true
        val elapsed = System.currentTimeMillis() - windowStart.toDate().time
        return elapsed >= WINDOW_MS
    }

    private fun resetAtFor(windowStart: Timestamp?): Timestamp? {
        if (windowStart == null) return null
        return Timestamp(java.util.Date(windowStart.toDate().time + WINDOW_MS))
    }

    suspend fun getStatus(user: User): ChatQuotaStatus {
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

            if (isExpired(windowStart)) {
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

    fun statusFlow(user: User): Flow<ChatQuotaStatus> = callbackFlow {
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

    /**
     * Attempts to consume one chat query for the given user.
     * Returns Result.success(ChatQuotaStatus) reflecting the new count if allowed,
     * or Result.failure with a user-friendly message if the free limit is reached.
     */
    suspend fun tryConsume(user: User): Result<ChatQuotaStatus> {
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

            val (newWindowStart, newCount) = if (isExpired(windowStart)) {
                Timestamp.now() to 1
            } else {
                val updated = count + 1
                if (updated > FREE_DAILY_LIMIT) {
                    return Result.failure(
                        Exception(
                            "You've reached your Free plan limit of $FREE_DAILY_LIMIT chat questions for today. Upgrade to Premium or try again later."
                        )
                    )
                }
                (windowStart ?: Timestamp.now()) to updated
            }

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
