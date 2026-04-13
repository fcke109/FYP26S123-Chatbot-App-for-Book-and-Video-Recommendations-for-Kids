package com.kidsrec.chatbot.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.kidsrec.chatbot.data.model.Feedback
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeedbackManager @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    suspend fun submitFeedback(feedback: Feedback): Result<Unit> {
        return runCatching {
            val docRef = firestore.collection("feedback").document()

            val payload = hashMapOf(
                "userId" to feedback.userId,
                "userName" to feedback.userName,
                "userEmail" to feedback.userEmail,
                "category" to feedback.category,
                "rating" to feedback.rating,
                "message" to feedback.message,
                "status" to "NEW",
                "createdAt" to Timestamp.now(),
                "reviewedAt" to null,
                "adminNote" to null
            )

            docRef.set(payload).await()
        }
    }

    fun getFeedbackFlow(): Flow<List<Feedback>> = callbackFlow {
        val registration = firestore.collection("feedback")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val items = snapshot?.documents.orEmpty().map { doc ->
                    Feedback(
                        id = doc.id,
                        userId = doc.getString("userId").orEmpty(),
                        userName = doc.getString("userName").orEmpty(),
                        userEmail = doc.getString("userEmail").orEmpty(),
                        category = doc.getString("category").orEmpty(),
                        rating = (doc.getLong("rating") ?: 5L).toInt(),
                        message = doc.getString("message").orEmpty(),
                        status = doc.getString("status") ?: "NEW",
                        createdAtMillis = doc.getTimestamp("createdAt")?.toDate()?.time ?: 0L,
                        reviewedAtMillis = doc.getTimestamp("reviewedAt")?.toDate()?.time,
                        adminNote = doc.getString("adminNote")
                    )
                }

                trySend(items)
            }

        awaitClose { registration.remove() }
    }

    suspend fun updateFeedbackStatus(
        feedbackId: String,
        status: String,
        adminNote: String? = null
    ): Result<Unit> {
        return runCatching {
            firestore.collection("feedback")
                .document(feedbackId)
                .update(
                    mapOf(
                        "status" to status,
                        "adminNote" to adminNote,
                        "reviewedAt" to Timestamp.now()
                    )
                )
                .await()
        }
    }
}