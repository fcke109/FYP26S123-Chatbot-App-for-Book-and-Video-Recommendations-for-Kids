package com.kidsrec.chatbot.data.repository

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.kidsrec.chatbot.data.model.TopDropOff
import com.kidsrec.chatbot.data.model.TopSearchedTopic
import com.kidsrec.chatbot.data.model.TopViewedBook
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    fun getTopSearchedTopicsFlow(): Flow<List<TopSearchedTopic>> = callbackFlow {
        val listener = firestore.collection("analyticsSearches")
            .orderBy("count", Query.Direction.DESCENDING)
            .limit(10)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("AnalyticsRepo", "Error loading top searches", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val topics = snapshot?.documents?.map { doc ->
                    TopSearchedTopic(
                        query = doc.getString("query") ?: doc.id,
                        count = doc.getLong("count") ?: 0
                    )
                } ?: emptyList()
                trySend(topics)
            }
        awaitClose { listener.remove() }
    }

    fun getTopViewedBooksFlow(): Flow<List<TopViewedBook>> = callbackFlow {
        val listener = firestore.collection("analyticsBookViews")
            .orderBy("viewCount", Query.Direction.DESCENDING)
            .limit(10)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("AnalyticsRepo", "Error loading top viewed books", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val books = snapshot?.documents?.map { doc ->
                    TopViewedBook(
                        bookId = doc.getString("bookId") ?: doc.id,
                        bookTitle = doc.getString("bookTitle") ?: "",
                        viewCount = doc.getLong("viewCount") ?: 0
                    )
                } ?: emptyList()
                trySend(books)
            }
        awaitClose { listener.remove() }
    }

    fun getTopDropOffsFlow(): Flow<List<TopDropOff>> = callbackFlow {
        val listener = firestore.collection("analyticsDropOffs")
            .orderBy("dropOffCount", Query.Direction.DESCENDING)
            .limit(10)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("AnalyticsRepo", "Error loading drop-offs", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val dropOffs = snapshot?.documents?.map { doc ->
                    TopDropOff(
                        itemId = doc.getString("itemId") ?: doc.id,
                        itemTitle = doc.getString("itemTitle") ?: "",
                        avgDurationSeconds = (doc.getLong("avgDurationSeconds") ?: 0).toInt(),
                        dropOffCount = doc.getLong("dropOffCount") ?: 0
                    )
                } ?: emptyList()
                trySend(dropOffs)
            }
        awaitClose { listener.remove() }
    }

    suspend fun trackSearch(query: String, userId: String) {
        try {
            val docRef = firestore.collection("analyticsSearches").document(query)
            val doc = docRef.get().await()
            if (doc.exists()) {
                docRef.update("count", (doc.getLong("count") ?: 0) + 1).await()
            } else {
                docRef.set(mapOf("query" to query, "count" to 1L, "lastSearchedBy" to userId)).await()
            }
        } catch (e: Exception) {
            Log.e("AnalyticsRepo", "Failed to track search: $query", e)
        }
    }

    suspend fun trackBookView(bookId: String, bookTitle: String, userId: String) {
        try {
            val docId = bookId.ifBlank { bookTitle.take(50) }
            val docRef = firestore.collection("analyticsBookViews").document(docId)
            val doc = docRef.get().await()
            if (doc.exists()) {
                docRef.update("viewCount", (doc.getLong("viewCount") ?: 0) + 1).await()
            } else {
                docRef.set(
                    mapOf(
                        "bookId" to bookId,
                        "bookTitle" to bookTitle,
                        "viewCount" to 1L,
                        "lastViewedBy" to userId
                    )
                ).await()
            }
        } catch (e: Exception) {
            Log.e("AnalyticsRepo", "Failed to track book view: $bookTitle", e)
        }
    }

    suspend fun trackDropOffPoint(
        itemId: String,
        itemTitle: String,
        userId: String,
        openedAt: Timestamp,
        closedAt: Timestamp,
        durationSeconds: Long
    ) {
        try {
            // Store individual drop-off event
            firestore.collection("analyticsDropOffEvents").add(
                mapOf(
                    "itemId" to itemId,
                    "itemTitle" to itemTitle,
                    "userId" to userId,
                    "openedAt" to openedAt,
                    "closedAt" to closedAt,
                    "durationSeconds" to durationSeconds
                )
            ).await()

            // Update aggregate drop-off stats
            val docRef = firestore.collection("analyticsDropOffs").document(itemId.ifBlank { itemTitle.take(50) })
            val doc = docRef.get().await()
            if (doc.exists()) {
                val currentCount = doc.getLong("dropOffCount") ?: 0
                val currentAvg = (doc.getLong("avgDurationSeconds") ?: 0).toInt()
                val newCount = currentCount + 1
                val newAvg = ((currentAvg * currentCount) + durationSeconds) / newCount
                docRef.update(
                    mapOf(
                        "dropOffCount" to newCount,
                        "avgDurationSeconds" to newAvg.toInt()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "itemId" to itemId,
                        "itemTitle" to itemTitle,
                        "dropOffCount" to 1L,
                        "avgDurationSeconds" to durationSeconds
                    )
                ).await()
            }
        } catch (e: Exception) {
            Log.e("AnalyticsRepo", "Failed to track drop-off: $itemTitle", e)
        }
    }
}
