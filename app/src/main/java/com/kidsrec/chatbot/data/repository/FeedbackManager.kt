package com.kidsrec.chatbot.data.repository

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.kidsrec.chatbot.data.model.Feedback
import com.kidsrec.chatbot.data.model.RecommendationType
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
    private val collection = firestore.collection("feedback")

    fun getUserFeedbackFlow(userId: String): Flow<List<Feedback>> = callbackFlow {
        val listener = collection
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FeedbackManager", "Listen failed", error)
                    return@addSnapshotListener
                }
                val items = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Feedback::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(items)
            }
        awaitClose { listener.remove() }
    }

    suspend fun submitFeedback(
        userId: String,
        recommendationId: String,
        recommendationTitle: String,
        recommendationType: RecommendationType,
        isPositive: Boolean
    ): Result<String> {
        return try {
            val docRef = collection.document()
            val feedback = Feedback(
                id = docRef.id,
                userId = userId,
                recommendationId = recommendationId,
                recommendationTitle = recommendationTitle,
                recommendationType = recommendationType,
                isPositive = isPositive,
                timestamp = Timestamp.now()
            )
            docRef.set(feedback).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e("FeedbackManager", "submitFeedback failed", e)
            Result.failure(e)
        }
    }

    suspend fun removeFeedback(feedbackId: String): Result<Unit> {
        return try {
            collection.document(feedbackId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FeedbackManager", "removeFeedback failed", e)
            Result.failure(e)
        }
    }
}
